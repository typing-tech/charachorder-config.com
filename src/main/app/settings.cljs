(ns app.settings
  (:refer-clojure :exclude [get])
  (:require
   [clojure.core :as core]
   [clojure.edn :as edn]
   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]))

(def ^js local-storage (oget js/window "localStorage"))

(defn get [k not-found]
  (let [v (.getItem local-storage (pr-str k))]
    (if v
      (edn/read-string v)
      not-found)))

(defn set! [k v]
  (let [k (pr-str k)
        v (pr-str v)]
    (.setItem local-storage k v)))
