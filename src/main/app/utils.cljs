(ns app.utils
  (:require
   ["date-fns" :as date-fns]
   [clojure.set :as set]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-main-root-element []
  (js/document.getElementById "charachorder-config"))

(defn bimap [m]
  (merge m (set/map-invert m)))

(defn timestamp-ms []
  (.now js/Date))

(defn human-time-with-seconds
  ([] (human-time-with-seconds (timestamp-ms)))
  ([t] (date-fns/formatISO9075 t)))
