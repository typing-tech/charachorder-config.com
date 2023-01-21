(ns app.csv
  (:require
   ["file-saver" :as file-saver]
   [clojure.string :as str]
   [datascript.core :as ds]
   [posh.reagent :as posh :refer [transact! pull q]]
   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [testdouble.cljs.csv :as csv]
   [app.macros :as mac :refer-macros [cond-xlet ->hash]]
   [app.ratoms :refer [*url-search-params]]
   [app.hw.cc1 :as cc1]
   [app.db :as db :refer [*db]]
   [app.serial.constants :refer [get-port]]))

(defn set-url! [csv]
  (let [current-layout (when (.has @*url-search-params "cc1-layout")
                         (.get @*url-search-params "cc1-layout"))]
    (when (not= csv current-layout)
      (let [encoded-layout (js/encodeURIComponent csv)
            url (str "?cc1-layout=" encoded-layout)]
        (.pushState js/window.history #js {} "" url)))))

(defn load-csv-text! [port-id csv]
  (let [csv (str/replace csv #"\r\n" "\n")
        xs (csv/read-csv csv)
        txs (mapv (fn [[layer location code]]
                    (let [switch-key-id (get cc1/location->switch-key-id location)
                          attr-ns (str layer "." switch-key-id)
                          attr (keyword attr-ns "code")]
                      [:db/add [:port/id port-id] attr code]))
                  xs)]
    (transact! *db txs)
    ;; assume success
    (set-url! csv)))

(defn on-drag-over! [e]
  (.preventDefault e)
  (.stopPropagation e)
  (oset! e "dataTransfer.dropEffect" "copy"))

(defn read-dropped-keymap-csv! [port-id
                                ^js e]
  (.preventDefault e)
  (.stopPropagation e)
  (cond-xlet
   :let [{:keys [*ready]} (get-port port-id)]
   (or (not *ready) (not @*ready))
   (transact! *db [[:db/add -1 :error/fatal
                    "Not ready to process CSV just yet. Please wait a few seconds."]])

   :let [files (oget e "dataTransfer.files")
         file (first files)]

   (not file) nil
   (not (str/ends-with? (str/lower-case (oget file "name")) ".csv")) nil

   :do (js/console.log file)
   :let [fr (new js/FileReader)]
   :do (.addEventListener fr "load" #(load-csv-text! port-id (oget fr "result")) false)
   :return (.readAsText fr file)))

(defn compute-csv [port-id]
  (let [m (ds/pull @*db '[*] [:port/id port-id])
        xs (mapv (fn [[layer switch-key-id]]
                   (let [loc (get-in cc1/switch-keys [switch-key-id :location])
                         attr-ns (str layer "." switch-key-id)
                         attr (keyword attr-ns "code")
                         code (get m attr)]
                     [layer loc code]))
                 cc1/layers+sorted-switch-key-ids)
        csv (csv/write-csv xs)]
    csv))

(defn update-url-from-db!
  "synchronous, not a Promise, not a channel."
  [port-id]
  (let [csv (compute-csv port-id)]
    (set-url! csv)))

(defn download-csv! [port-id]
  (let [csv (compute-csv port-id)
        blob (new js/Blob
                  #js [csv]
                  #js {:type "text/plain;charset=utf-8"})]
    (.saveAs file-saver blob "cc1-layout.csv")
    nil))
