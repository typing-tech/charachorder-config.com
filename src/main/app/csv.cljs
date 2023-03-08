(ns app.csv
  (:require
   ["file-saver" :as file-saver]
   ["snappyjs" :as snappyjs]
   ["rfc4648" :refer [base64url]]

   [clojure.string :as str]
   [datascript.core :as ds]
   [posh.reagent :as posh :refer [transact! pull q]]
   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [testdouble.cljs.csv :as csv]
   [app.macros :as mac :refer-macros [cond-xlet ->hash]]
   [app.ratoms :refer [*url-search-params]]
   [app.preds :refer [is-device-cc1?
                      is-device-cc-lite?]]
   [app.hw :refer [get-hw-switch-keys
                   get-hw-location->switch-key-id
                   get-hw-layers+sorted-switch-key-ids]]
   [app.db :as db :refer [*db]]
   [app.serial.constants :refer [get-port dummy-port-id]]
   [app.serial.ops :as ops]))

(defn compressed-text->csv [text]
  (cond-xlet
   ; :do (js/console.trace "compressed-text->csv")
   (nil? text) nil
   (str/blank? text) nil
   :let [decoder (new js/TextDecoder "utf-8")]
   :return (->> text
                ((oget base64url "parse"))
                ((oget snappyjs "uncompress"))
                (.decode decoder))))

(defn csv->compressed-text [csv]
  (-> (new js/TextEncoder "utf-8")
      (.encode csv)
      ((oget snappyjs "compress"))
      ((oget base64url "stringify"))))

(defn set-url! [csv]
  (dorun
   (for [param ["cc-lite-layout" "cc1-layout"]]
     (when (.has @*url-search-params param)
       (let [current-layout (-> (.get @*url-search-params param)
                                (compressed-text->csv))]
         (when (not= csv current-layout)
           (let [encoded-layout (csv->compressed-text csv)
                 url (str "?" param "="  encoded-layout)]
             ; (js/console.log (count encoded-layout))
             (.pushState js/window.history #js {} "" url))))))))

(defn load-csv-text! [port-id csv]
  (let [port (get-port port-id)
        location->switch-key-id (get-hw-location->switch-key-id port)

        csv (str/replace csv #"\r\n" "\n")
        xs (->> (csv/read-csv csv)
                (remove (partial = [""])))
        txs (mapv (fn [[layer location code]]
                    (let [switch-key-id (get location->switch-key-id location)
                          attr-ns (str layer "." switch-key-id)
                          attr (keyword attr-ns "code")]
                      [:db/add [:port/id port-id] attr code]))
                  xs)]
    (transact! *db txs)
    (when (not= port-id dummy-port-id)
      (dorun
       (mapv (fn [[layer location code]]
               (let [switch-key-id (get location->switch-key-id location)]
                 (ops/set-keymap! port-id layer switch-key-id code)))
             xs)))
    ;; assume success
    (set-url! csv)))

(defn load-compressed-csv-text! [port-id text]
  (->> (compressed-text->csv text)
       (load-csv-text! port-id)))

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
   (and (not= port-id dummy-port-id)
        (or (not *ready) (not @*ready)))
   (transact! *db [[:db/add -1 :error/error
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
  (let [port (get-port port-id)
        switch-keys (get-hw-switch-keys port)
        layers+sorted-switch-key-ids (get-hw-layers+sorted-switch-key-ids port)

        m (ds/pull @*db '[*] [:port/id port-id])
        xs (mapv (fn [[layer switch-key-id]]
                   (let [loc (get-in switch-keys [switch-key-id :location])
                         attr-ns (str layer "." switch-key-id)
                         attr (keyword attr-ns "code")
                         code (get m attr)]
                     [layer loc code]))
                 layers+sorted-switch-key-ids)
        csv (csv/write-csv xs)]
    csv))

(defn update-url-from-db!
  "synchronous, not a Promise, not a channel."
  [port-id]
  (let [csv (compute-csv port-id)]
    (set-url! csv)))

(defn download-csv! [port-id]
  (let [port (get-port port-id)
        csv (compute-csv port-id)
        blob (new js/Blob
                  #js [csv]
                  #js {:type "text/plain;charset=utf-8"})]
    (cond
      (is-device-cc1? port) (.saveAs file-saver blob "cc1-layout.csv")
      (is-device-cc-lite? port) (.saveAs file-saver blob "cc-lite-layout.csv"))

    nil))
