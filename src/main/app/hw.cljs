(ns app.hw
  (:require
   [app.preds :refer [is-device-cc1?]]
   [app.hw.cc1 :as cc1]))

(defn get-hw-switch-keys [port]
  (cond
    (is-device-cc1? port) cc1/switch-keys
    :else {}))

(defn get-hw-layers+sorted-switch-key-ids [port]
  (cond
    (is-device-cc1? port) cc1/layers+sorted-switch-key-ids
    :else []))
