(ns app.csv
  (:require
   [clojure.string :as str]
   [posh.reagent :as posh :refer [transact! pull q]]
   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [testdouble.cljs.csv :as csv]
   [app.hw.cc1 :as cc1]
   [app.db :as db :refer [*db]]))

(defn load-csv-text! [port-id csv]
  (let [xs (csv/read-csv csv)
        txs (mapv (fn [[layer location code]]
                    (let [switch-key-id (get cc1/location->switch-key-id location)
                          attr-ns (str layer "." switch-key-id)
                          attr (keyword attr-ns "code")]
                      [:db/add [:port/id port-id] attr code]))
                  xs)]
    (transact! *db txs)
    ;; assume success
    (let [encoded-layout (js/encodeURIComponent csv)
          url (str "?cc1-layout=" encoded-layout)]
      (.pushState js/window.history #js {} "" url))
    ))

(defn on-drag-over! [e]
  (.preventDefault e)
  (.stopPropagation e)
  (oset! e "dataTransfer.dropEffect" "copy"))

(defn read-dropped-keymap-csv! [port-id
                                ^js e]
  (.preventDefault e)
  (.stopPropagation e)
  (let [files (oget e "dataTransfer.files")
        file (first files)]
    (when (and file (str/ends-with? (str/lower-case (oget file "name"))
                                    ".csv"))
      (js/console.log file)
      (let [fr (new js/FileReader)]
        (.addEventListener fr "load" #(load-csv-text! port-id (oget fr "result")) false)
        (.readAsText fr file)))))

