(ns app.db
  (:require
   [datascript.core :as ds]
   [posh.reagent :as posh :refer [transact! posh!]]))

(def main-db-schema
  {:port/id {:db/cardinality :db.cardinality/one :db/unique :db.unique/identity}})

(defonce *db (ds/create-conn main-db-schema))

(defn init! []
  (posh! *db))
