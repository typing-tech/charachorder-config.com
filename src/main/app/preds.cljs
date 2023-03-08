(ns app.preds
  (:require
   [clojure.string :as str]
   [app.ratoms :refer [*url-search-params
                       *num-devices-connected
                       *active-port-id]]))

(defn is-device-not-yet-determined? [{:as port :keys [*device-name]}]
  (and port (= @*device-name "???")))

(defn is-device-cc1? [{:as port :keys [*device-name]}]
  (and (or port (and (not port) (not (.has @*url-search-params "cc-lite-layout"))))
       (or (and (not port) (.has @*url-search-params "cc1-layout"))
           (str/starts-with? @*device-name "CHARACHORDER ONE "))))

(defn is-device-cc-lite? [{:as port :keys [*device-name]}]
  (and (or port (and (not port) (not (.has @*url-search-params "cc1-layout"))))
       (or (and (not port) (.has @*url-search-params "cc-lite-layout"))
           (str/starts-with? @*device-name "CHARACHORDER LITE "))))
