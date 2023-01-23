(ns app.serial.ops
  (:require
   [cljs.core.async :as async
    :refer [chan <! >! onto-chan! close! put!]
    :refer-macros [go go-loop]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [clojure.string :as str]
   [posh.reagent :as posh :refer [transact!]]
   [datascript.core :as ds]

   [app.ratoms :refer [*num-device-connected *active-port-id]]
   [app.db :refer [*db]]
   [app.hw.cc1 :as cc1]
   [app.serial.constants :refer [baud-rates
                                 *ports
                                 get-port
                                 dummy-port-id]]
   [app.serial.fns :as fns]))

(defn disconnect! [port-id]
  (let [{:keys [close-port-and-cleanup!]} (get-port port-id)]
    (reset! *active-port-id nil)
    (close-port-and-cleanup!)))

(defn refresh-params [port-id]
  (let [port (get-port port-id)]
    (fns/query-all-var-params! port)))

(defn reset-params! [port-id]
  (let [{:keys [close-port-and-cleanup! fn-ch]} (get-port port-id)]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
                (>! write-ch "RST PARAMS")
                (let [ret (<! read-ch)]
                  (js/console.log ret))
                (reset! *active-port-id nil)
                (close-port-and-cleanup!)))]
      (put! fn-ch f))))

(defn factory-reset! [port-id]
  (let [{:keys [close-port-and-cleanup! fn-ch]} (get-port port-id)
        cmd "RST FACTORY"]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
               (js/console.log )
                (>! write-ch "RST FACTORY")
                (let [ret (<! read-ch)]
                  (js/console.log ret))
                (reset! *active-port-id nil)
                (close-port-and-cleanup!)))]
      (put! fn-ch f))))

(defn reset-keymaps! [port-id]
  (let [{:keys [close-port-and-cleanup! fn-ch]} (get-port port-id)]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
                (>! write-ch "RST KEYMAPS")
                (let [ret (<! read-ch)]
                  (js/console.log ret))
                (reset! *active-port-id nil)
                (close-port-and-cleanup!)))]
      (put! fn-ch f))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn commit! [port-id]
  (let [{:as port :keys [close-port-and-cleanup! fn-ch]} (get-port port-id)
        cmd (fns/cmd-var-commit)

        m (ds/pull @*db '[*] [:port/id port-id])
        attr-nses (map (fn [[layer switch-key-ids]]
                         (str layer "." switch-key-ids))
                       cc1/layers+sorted-switch-key-ids)
        txs (reduce (fn [txs attr-ns]
                      (let [a (get m (keyword attr-ns "code"))
                            hw-code-key (keyword attr-ns "hw.code")
                            b (get m hw-code-key)]
                        (if (not= a b)
                          (conj txs [:db/add [:port/id port-id] hw-code-key a])
                          txs)))
                    []
                    attr-nses)]
    ; (js/console.log txs)
    (letfn [(f [{:keys [write-ch read-ch ->console]}]
              (go
                (>! write-ch cmd)
                (let [ret (<! read-ch)
                      {:keys [success]} (fns/parse-commit-ret ret)]
                  (js/console.log ret)
                  (if success
                    (->console "COMMIT success")
                    (->console "COMMIT ERROR"))
                  (when success (transact! *db txs))
                  success)))]
      (put! fn-ch f))))

(defn set-keymap! [port-id layer switch-key-id code]
  (assert layer)
  (assert (string? switch-key-id))
  (assert code)
  (assert (string? switch-key-id))
  (let [{:as port :keys [fn-ch]} (get-port port-id)
        location (get-in cc1/switch-keys [switch-key-id :location])
        cmd (fns/cmd-var-set-keymap layer location code)]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
                (js/console.log "SEND" cmd)
                (>! write-ch cmd)
                (let [ret (<! read-ch)
                      {:keys [success]} (fns/parse-var-set-keymap-ret ret)]
                  (js/console.log "RECV" ret)
                  (if success
                    (js/console.log "set keymap success")
                    (js/console.error "set keymap ERROR")))))]
      (put! fn-ch f))))