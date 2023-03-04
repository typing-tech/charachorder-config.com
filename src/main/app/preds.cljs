(ns app.preds
  (:require
   [clojure.string :as str]))

(defn is-device-not-yet-determined? [{:keys [*device-name]}]
  (= @*device-name "???"))

(defn is-device-cc1? [{:keys [*device-name]}]
  (str/starts-with? @*device-name "CHARACHORDER ONE "))
