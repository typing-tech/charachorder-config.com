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
   [app.db :refer [*db]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; from SerialAPI.md
(def cmd-var-commit "B0")
(def cmd-var-get-parameter "B1")
(def cmd-var-set-parameter "B2")
(def cmd-var-get-keymap "B3")
(def cmd-var-set-keymap "B4")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn store-device-name [{:keys [read-ch write-ch *device-name]}]
  (go
    (>! write-ch "ID")
    (let [ret (<! read-ch)
          dev-name (if (str/starts-with? ret "ID ")
                     (subs ret (count "ID "))
                     ret)]
      (reset! *device-name dev-name))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn issue-connect-cmds! [{:keys [fn-ch]}]
  (go
    (>! fn-ch store-device-name)))
