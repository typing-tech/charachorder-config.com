(ns app.preds
  (:require
   [clojure.string :as str]
   [app.ratoms :refer [*url-search-params
                       *num-devices-connected
                       *active-port-id]]))

(defn is-device-not-yet-determined? [{:as port :keys [*device-name]}]
  (and port (= @*device-name "???")))

(defn is-device-cc1? [{:as port :keys [*device-name]}]
  (or (and (not port)
           (.has @*url-search-params "cc1-layout"))
      (str/starts-with? @*device-name "CHARACHORDER ONE ")))
