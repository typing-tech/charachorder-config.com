(ns app.serial.fns
  "All fns here MUST return a channel!"
  (:require
   [cljs.core.async :as async
    :refer [chan <! >! onto-chan! close!]
    :refer-macros [go go-loop]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [clojure.string :as str]

   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [promesa.core :as p]
   [datascript.core :refer [squuid]]
   [posh.reagent :as posh :refer [transact!]]

   [app.macros :refer-macros [cond-xlet ->hash]]
   [app.ratoms :refer [*active-port-id]]
   [app.db :refer [*db]]

   [app.hw :refer [get-hw-switch-keys
                   get-hw-layers+sorted-switch-key-ids]]
   [app.codes :refer [var-subcmds var-params code->var-param]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cmd-var-get-parameter [param]
  (let [arg0 (:get-parameter var-subcmds)
        arg1 (get-in var-params [param :code])]
    (assert arg0)
    (assert arg1)
    (->> ["VAR" arg0 arg1]
         (interpose " ")
         (apply str))))

(defn parse-get-parameter-ret [ret]
  (let [[cmd-code subcmd-code param-code raw-data success-str] (str/split ret #"\s+")
        success (js/parseInt success-str)
        success (if (< 0 success) false true)
        {param-type :type
         param :param} (get code->var-param param-code)
        data (when success
               (case param-type
                 :num-boolean (case raw-data
                                "0" false
                                "1" true)
                 raw-data))]
    (->hash param cmd-code subcmd-code param-code raw-data data success)))

(defn store-device-name [{:keys [read-ch write-ch *device-name]}]
  (go
    (>! write-ch "ID")
    (let [ret (<! read-ch)
          dev-name (if (str/starts-with? ret "ID ")
                     (subs ret (count "ID "))
                     ret)]
      (reset! *device-name dev-name))))

(defn store-device-version [{:keys [read-ch write-ch *device-version]}]
  (go
    (>! write-ch "VERSION")
    (let [ret (<! read-ch)
          version (if (str/starts-with? ret "VERSION ")
                    (subs ret (count "VERSION "))
                    ret)]
      ;; fix to read stray "-beta line after VERSION return line"
      (when (= ret "VERSION 0.9.17")
        (>! write-ch "ID")
        (<! read-ch))
      (reset! *device-version version))))

(defn gen-var-get-param-fn [{:keys []} param]
  (fn [{:keys [port-id read-ch write-ch]}]
    (assert port-id)
    (go
      (>! write-ch (cmd-var-get-parameter param))
      (let [ret (<! read-ch)
            {:as m :keys [param data]} (parse-get-parameter-ret ret)
            port [:port/id port-id]]
        ; (js/console.log m)
        (when-not (nil? data)
          (let [tx-data [[:db/add port param data]]]
            ; (js/console.log tx-data)
            (transact! *db tx-data)))))))

(defn query-all-var-params! [{:as port :keys [fn-ch]}]
  (let [params (->> (map key var-params))
        fns (map (partial gen-var-get-param-fn port) params)]
    (onto-chan! fn-ch fns false)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cmd-var-get-keymap [layer loc]
  (let [arg0 (:get-keymap var-subcmds)]
    (assert arg0)
    (->> ["VAR" arg0 layer loc]
         (interpose " ")
         (apply str))))

(defn parse-get-keymap-ret [ret]
  (let [[cmd-code subcmd-code layer location code success-str]
        (str/split ret #"\s+")
        success (js/parseInt success-str)
        success (if (< 0 success) false true)
        code (if success code nil)]
    (->hash cmd-code subcmd-code layer location code)))

(defn gen-var-get-keymap-fn [{:keys []}
                             {:keys [boot] :or {boot false}}
                             [layer loc hw-attr attr]]
  (fn [{:keys [port-id read-ch write-ch]}]
    (assert port-id)
    (go
      (>! write-ch (cmd-var-get-keymap layer loc))
      (let [ret (<! read-ch)
            {:as m :keys [layer loc code]} (parse-get-keymap-ret ret)
            port [:port/id port-id]]
        ; (js/console.log m)
        (when-not (nil? code)
          (let [tx-data (cond-> [[:db/add port attr code]]
                          boot (conj [:db/add port hw-attr code]))]
            (js/console.log (pr-str tx-data))
            (transact! *db tx-data)))))))

(defn query-all-var-keymaps!
  "Returns a channel."
  [{:as port :keys [fn-ch]} & {:as opts
                               :keys [boot]
                               :or {boot false}}]
  (let [switch-keys (get-hw-switch-keys port)
        layers+sorted-switch-key-ids (get-hw-layers+sorted-switch-key-ids port)

        xs (mapv (fn [[layer switch-key-id]]
                   (let [loc (get-in switch-keys [switch-key-id :location])
                         attr-ns (str layer "." switch-key-id)
                         hw-attr (keyword attr-ns "hw.code")
                         attr (keyword attr-ns "code")]
                     [layer loc hw-attr attr]))
                 layers+sorted-switch-key-ids)
        fns (map (partial gen-var-get-keymap-fn port opts) xs)]
    (onto-chan! fn-ch fns false)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cmd-var-commit []
  (let [arg0 (:commit var-subcmds)]
    (assert arg0)
    (->> ["VAR" arg0]
         (interpose " ")
         (apply str))))

(defn parse-commit-ret [ret]
  (let [[cmd-code subcmd-code success-str] (str/split ret #"\s+")
        success (js/parseInt success-str)
        success (if (< 0 success) false true)]
    (->hash cmd-code subcmd-code success)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cmd-var-set-keymap [layer loc code]
  (let [arg0 (:set-keymap var-subcmds)]
    (assert arg0)
    (assert layer)
    (assert loc)
    (assert code)
    (->> ["VAR" arg0 layer loc code]
         (interpose " ")
         (apply str))))

(defn parse-var-set-keymap-ret [ret]
  (let [[cmd-code subcmd-code layer location code success-str] (str/split ret #"\s+")
        success (js/parseInt success-str)
        success (if (< 0 success) false true)]
    (->hash cmd-code subcmd-code layer location code success)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn issue-connect-cmds! [{:as port :keys [fn-ch *ready *device-name]}]
  (go
    (>! fn-ch store-device-name)
    (>! fn-ch store-device-version)
    (<! (query-all-var-params! port))
    (<! (query-all-var-keymaps! port :boot true))
    (reset! *ready true)))
