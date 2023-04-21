(ns app.csv
  (:require ["file-saver" :as file-saver]
            ["rfc4648" :refer [base64url]]
            ["snappyjs" :as snappyjs]
            [app.components :refer [button]]
            [app.db :as db :refer [*db]]
            [app.hw :refer [get-hw-layers+sorted-switch-key-ids
                            get-hw-location->switch-key-id get-hw-switch-keys]]
            [app.macros :as mac :refer-macros [cond-xlet ->hash]]
            [app.preds :refer [is-device-cc-lite? is-device-cc1?]]
            [app.ratoms :refer [*url-search-params]]
            [app.serial.constants :refer [dummy-port-id get-port]]
            [app.serial.ops :as ops]
            [clojure.string :as str]
            [datascript.core :as ds :refer [squuid]]
            [oops.core :refer [oapply oapply! oapply!+ oapply+ ocall ocall!
                               ocall!+ ocall+ oget oget+ oset! oset!+]]
            [posh.reagent :as posh :refer [pull q transact!]]
            [testdouble.cljs.csv :as csv]))

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

(defn set-url! [port-id csv]
  (cond-xlet
   :let [port (get-port port-id)]
   (is-device-cc1? port) (let [param "cc1-layout"
                               current-layout (when (.has @*url-search-params param)
                                                (-> (.get @*url-search-params param)
                                                    (compressed-text->csv)))]
                           (when (not= csv current-layout)
                             (let [encoded-layout (csv->compressed-text csv)
                                   url (str "?" param "="  encoded-layout)]
                               (.pushState js/window.history #js {} "" url))))
   (is-device-cc-lite? port) (let [param "cc-lite-layout"
                                   current-layout (when (.has @*url-search-params param)
                                                    (-> (.get @*url-search-params param)
                                                        (compressed-text->csv)))]
                               (when (not= csv current-layout)
                                 (let [encoded-layout (csv->compressed-text csv)
                                       url (str "?" param "="  encoded-layout)]
                                   (.pushState js/window.history #js {} "" url))))))

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
    ; (js/console.log txs)
    (transact! *db txs)
    (when (not= port-id dummy-port-id)
      (dorun
       (mapv (fn [[layer location code]]
               (let [switch-key-id (get location->switch-key-id location)]
                 (ops/set-keymap! port-id layer switch-key-id code)))
             xs)))
    ;; assume success
    (set-url! port-id csv)))

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
    (set-url! port-id csv)))

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

(defn upload-csv-button [port-id]
  (let [input-id (squuid)]
    (fn []
      (let [on-change!
            (fn [e]
              (let [file (-> e .-target .-files (aget 0))]
                (when file
                  (let [reader (new js/FileReader)]
                    (set! (.-onload reader)
                          (fn [e]
                            (let [csv (-> e .-target .-result)]
                              (load-csv-text! port-id csv))))
                    (.readAsText reader file)))))
            f
            (fn []
              (.click (js/document.getElementById input-id)))]
        [:<>
         [:input {:type "file" :class "dn" :id input-id
                  :on-change on-change!}]
         (button f
                 [[:span
                   "Upload and " [:br]
                   " Apply CSV"]]
                 :classes ["ma0"
                           "button-primary"
                           "button-small"
                           "button-success"])]))))
