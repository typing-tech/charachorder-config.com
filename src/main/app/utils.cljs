(ns app.utils
  (:require ["date-fns" :as date-fns]
            [app.codes :refer [code-int->keymap-code]]
            [app.macros :refer-macros [cond-xlet ->hash]]
            [cljs.cache :as cache]
            [clojure.set :as set]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-main-root-element []
  (js/document.getElementById "charachorder-config"))

(defn bimap [m]
  (merge m (set/map-invert m)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn binary->decimal [b]
  (js/parseInt b 2))

(defn parse-binary-chord-string
  "`chunks` is a vector of 12 action codes, each 10 bits long, represented as an integer."
  [full-bcs]
  (cond-xlet
   :do (assert (string? full-bcs))
   :do (assert (= 128 (count full-bcs)))
   :let [unused-binary-string (subs full-bcs 0 8)
         bcs (subs full-bcs 8)]
   :do (assert (= 120 (count bcs)))
   :let [bs (vec (for [i (range 12)]
                   (subs bcs (* 10 i) (* 10 (+ i 1)))))
         chunks (mapv binary->decimal bs)]
   :return (->hash unused-binary-string chunks)))
