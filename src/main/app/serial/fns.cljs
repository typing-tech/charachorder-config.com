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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn store-device-name [{:keys [read-ch write-ch *device-name]}]
  (go
    (>! write-ch "ID")
    (let [ret (<! read-ch)
          dev-name (if (str/starts-with? ret "ID ")
                     (subs ret (count "ID "))
                     ret)]
      (reset! *device-name dev-name))))

(defn nop [{:keys [read-ch write-ch]}]
  (go
    nil))

(defn gen-var-get-fn [param]
  (fn [{:keys [read-ch write-ch]}]
    (go
      (>! write-ch (cmd-var-get-parameter param))
      (let [ret (<! read-ch)
            m (parse-get-parameter-ret ret)]
        (js/console.log m)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn issue-connect-cmds! [{:keys [fn-ch]}]
  (go
    (>! fn-ch store-device-name)))

(defn query-all-vars! [{:as port :keys [fn-ch]}]
  (let [params (->> (map key var-params))
        fns (map gen-var-get-fn params)]
    (go
     (<! (onto-chan! fn-ch fns false)))))
