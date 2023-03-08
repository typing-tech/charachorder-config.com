(ns app.hw
  (:require
   [app.preds :refer [is-device-cc1?
                      is-device-cc-lite?]]
   [app.hw.cc1 :as cc1]
   [app.hw.cc-lite :as cc-lite]
   ))

(defn get-hw-switch-keys [port]
  (cond
    (is-device-cc1? port) cc1/switch-keys
    (is-device-cc-lite? port) cc-lite/switch-keys
    :else {}))

(defn get-hw-location->switch-key-id [port]
  (cond
    (is-device-cc1? port) cc1/location->switch-key-id
    (is-device-cc-lite? port) cc-lite/location->switch-key-id
    :else {}))

(defn get-hw-layers+sorted-switch-key-ids [port]
  (cond
    (is-device-cc1? port) cc1/layers+sorted-switch-key-ids
    (is-device-cc-lite? port) cc-lite/layers+sorted-switch-key-ids
    :else []))
