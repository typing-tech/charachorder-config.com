(ns app.utils
  (:require
   ["date-fns" :as date-fns]
   [clojure.set :as set]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-main-root-element []
  (js/document.getElementById "charachorder-config"))

(defn bimap [m]
  (merge m (set/map-invert m)))

(defn human-time-now-with-seconds []
  (date-fns/formatISO9075 (.now js/Date)))
