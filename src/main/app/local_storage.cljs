(ns app.local-storage
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def localStorage (.-localStorage js/window))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-item!
  "Set `key' in browser's localStorage to `val`."
  [k v]
  (let [k (pr-str k)
        v (pr-str v)]
    (.setItem localStorage k v)))

(defn get-item
  "Returns value of `key' from browser's localStorage."
  ([k] (get-item k nil))
  ([k not-found]
   (let [k (pr-str k)]
     (if-let [v (.getItem localStorage k)]
       (edn/read-string v)
       not-found))))

(defn remove-item!
  "Remove the browser's localStorage value for the given `key`"
  [k]
  (let [k (pr-str k)]
    (.removeItem localStorage k)))
