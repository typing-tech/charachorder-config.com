(ns app.serial.fns
  "All fns here MUST return a channel!"
  (:require [app.codes :refer [cml-subcmds code->var-param var-params
                               var-subcmds]]
            [app.db :refer [*db]]
            [app.hw :refer [get-hw-layers+sorted-switch-key-ids
                            get-hw-switch-keys]]
            [app.macros :refer-macros [cond-xlet ->hash]]
            [app.ratoms :refer [*active-port-id]]
            [cljs.core.async :as async
    :refer [<! >! chan close! onto-chan!]
    :refer-macros [go go-loop]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.string :as str]
            [datascript.core :refer [squuid]]
            [oops.core :refer [oapply oapply! oapply!+ oapply+ ocall ocall!
                               ocall!+ ocall+ oget oget+ oset! oset!+]]
            [posh.reagent :as posh :refer [transact!]]
            [promesa.core :as p]
            [goog.object :as go]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cmd-var-get-parameter [param]
  (let [arg0 (:get-parameter var-subcmds)
        arg1 (get-in var-params [param :code])]
    (assert arg0)
    (assert arg1)
    (->> ["VAR" arg0 arg1]
         (interpose " ")
         (apply str))))

(defn parse-var-get-parameter-ret [ret]
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

(defn cmd-var-set-parameter [param value]
  (let [arg0 (:set-parameter var-subcmds)
        arg1 (get-in var-params [param :code])
        arg2 (case (get-in var-params [param :type])
               :num-boolean (if value "1" "0")
               value)]
    (assert arg0)
    (assert arg1)
    (assert arg2)
    (->> ["VAR" arg0 arg1 arg2]
         (interpose " ")
         (apply str))))

(defn parse-var-set-parameter-ret [ret]
  (let [[cmd-code subcmd-code param-code data-out-str success-str]
        (str/split ret #"\s+")
        success (js/parseInt success-str)
        success (and (if (< 0 success) false true)
                     (not= data-out-str "00"))
        {param-type :type
         param :param} (get code->var-param param-code)
        data (when success
               (case param-type
                 :num-boolean (case data-out-str
                                "0" false
                                "1" true)
                 data-out-str))]
    (->hash param param-type
            cmd-code subcmd-code param-code data success)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
            {:as m :keys [param data]} (parse-var-get-parameter-ret ret)
            port [:port/id port-id]]
        ; (js/console.log m)
        (when-not (nil? data)
          (let [tx-data [[:db/add port param data]]]
            ; (js/console.log tx-data)
            (transact! *db tx-data)))))))

(defn query-var-param! [{:as port :keys [fn-ch]} param]
  (let [fns [(gen-var-get-param-fn port param)]]
    (onto-chan! fn-ch fns false)))

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
            ;; (js/console.log (pr-str tx-data))
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

(defn cmd-cml-get-chordmap-count []
  (let [arg0 (:get-chordmap-count cml-subcmds)]
    (assert arg0)
    (->> ["CML" arg0]
         (interpose " ")
         (apply str))))

(defn parse-cml-get-chordmap-count-ret [ret]
  (let [[cmd-code subcmd-code count-str] (str/split ret #"\s+")
        count (js/parseInt count-str)
        success (boolean count)]
    (->hash cmd-code subcmd-code count success)))

(defn cmd-cml-get-chordmap-by-index [index]
  (let [arg0 (:get-chordmap-by-index cml-subcmds)
        index-str (str index)]
    (assert arg0)
    (assert (number? index))
    (->> ["CML" arg0 index-str]
         (interpose " ")
         (apply str))))

(defn parse-cml-get-chordmap-by-index [ret]
  (let [[cmd-code subcmd-code index-str hex-chord-string phrase] (str/split ret #"\s+")
        index (js/parseInt index-str)
        success (and (not= "0" hex-chord-string)
                     (not= "0" phrase))]
    (->hash cmd-code subcmd-code index
            hex-chord-string phrase
            success)))

(defn gen-cml-get-chordmap-by-index-fn [index]
  (fn [{:keys [port-id read-ch write-ch *chord-read-index]}]
    (assert port-id)
    (go
      (>! write-ch (cmd-cml-get-chordmap-by-index index))
      (let [ret (<! read-ch)
            {:as m :keys [index hex-chord-string phrase success]}
            (parse-cml-get-chordmap-by-index ret)]
        (when success
          ;; (js/console.log m)
          (swap! *chord-read-index inc)
          (transact! *db [{:chord/id [port-id hex-chord-string]
                           :chord/port-id port-id
                           :chord/index index
                           :chord/hex-chord-string hex-chord-string
                           :chord/phrase phrase}]))))))

(defn cmd-cml-get-chordmap-by-chord [hex-chord-string]
  (let [arg0 (:get-chordmap-by-chord cml-subcmds)]
    (assert arg0)
    (assert hex-chord-string)
    (->> ["CML" arg0 hex-chord-string]
         (interpose " ")
         (apply str))))

(defn parse-cml-get-chordmap-by-chord-ret [ret]
  (let [[cmd-code subcmd-code hex-chord-string phrase] (str/split ret #"\s+")
        success (and (not= "0" hex-chord-string)
                     (not= "0" phrase)
                     (not= "2" phrase))]
    (->hash cmd-code subcmd-code hex-chord-string phrase success)))

(defn cmd-cml-set-chordmap-by-chord [hex-chord-string phrase]
  (let [arg0 (:set-chordmap-by-chord cml-subcmds)]
    (assert arg0)
    (assert hex-chord-string)
    (assert phrase)
    (->> ["CML" arg0 hex-chord-string phrase]
         (interpose " ")
         (apply str))))

(defn parse-cml-set-chordmap-by-chord-ret [ret]
  (let [[cmd-code subcmd-code hex-chord-string phrase success-str] (str/split ret #"\s+")
        success (js/parseInt success-str)
        success (if (< 0 success) false true)]
    (->hash cmd-code subcmd-code hex-chord-string phrase success)))

(defn cmd-cml-del-chordmap-by-chord [hex-chord-string]
  (let [arg0 (:del-chordmap-by-chord cml-subcmds)]
    (assert arg0)
    (assert hex-chord-string)
    (->> ["CML" arg0 hex-chord-string]
         (interpose " ")
         (apply str))))

(defn parse-cml-del-chordmap-by-chord-ret [ret]
  (let [[cmd-code subcmd-code hex-chord-string success-str] (str/split ret #"\s+")
        success (js/parseInt success-str)
        success (if (< 0 success) false true)]
    (->hash cmd-code subcmd-code hex-chord-string success)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
