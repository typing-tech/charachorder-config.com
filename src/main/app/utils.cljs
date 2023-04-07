(ns app.utils
  (:require
   ["date-fns" :as date-fns]
   [clojure.set :as set]
   [cljs.cache :as cache]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-main-root-element []
  (js/document.getElementById "charachorder-config"))

(defn bimap [m]
  (merge m (set/map-invert m)))

(defn timestamp-ms []
  (.now js/Date))

(def *human-time-cache (atom (cache/lru-cache-factory {} :threshold 64)))

(defn human-time-with-seconds* [t] (date-fns/formatISO9075 t))
(defn human-time-with-seconds
  ([] (human-time-with-seconds (timestamp-ms)))
  ([t]
   (swap! *human-time-cache
          #(cache/through human-time-with-seconds* % t))
   (get @*human-time-cache t)))
