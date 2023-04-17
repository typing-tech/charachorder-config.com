(ns app.views.chords 
  (:require [app.codes :refer [code-int->short-dom]]
            [app.components :refer [button]]
            [app.db :refer [*db]]
            [app.macros :refer-macros [->hash cond-xlet]]
            [app.serial.constants :refer [dummy-port-id get-port]]
            [app.serial.fns :refer [query-all-chordmaps!]]
            [app.utils :refer [hex-str->bin-str parse-binary-chord-string]]
            [goog.string :refer [format]]
            [posh.reagent :as posh]))

(defn chord-chunks-com [chunks]
  (let [chunks (filter #(not= 0 %) chunks)]
    (into
     [:div {:class "chord-chunks mh2"}]
     (for [chunk chunks]
       (-> (code-int->short-dom chunk)
           (assoc-in [1 :class] "chord-chunks__chunk"))))))

(defn get-chords [port-id]
  (->> @(posh/q '[:find ?e ?index ?hex-chord-string ?phrase
                  :in $ ?port-id
                  :where
                  [?e :chord/port-id ?port-id]
                  [?e :chord/index ?index]
                  [?e :chord/hex-chord-string ?hex-chord-string]
                  [?e :chord/phrase ?phrase]]
                *db port-id)
       (map #(zipmap [:e :index :hex-chord-string :phrase] %))
       (sort-by :index)))

(defn chords-table [{:keys [port-id *is-reading-chords *chord-read-index *num-chords]}]
  (if @*is-reading-chords
    [:div (format "Reading Chords (%d/%d)" @*chord-read-index @*num-chords)]
    (let [chords (get-chords port-id)]
      [:table {:class "pure-table pure-table-horizontal"}
       [:thead
        [:tr
         [:th "Index"]
    ;;  [:th "Hex Chord String"]
    ;;  [:th "Chunks"]
         [:th "Chord"]
         [:th "Phrase"]]]
       [:tbody
        (for [{:keys [e index hex-chord-string phrase]} chords]
          (let [chunks (->> hex-chord-string
                            hex-str->bin-str
                            parse-binary-chord-string
                            :chunks)]
            [:tr {:key e}
             [:td index]
        ;; [:td hex-chord-string]
        ;; [:td (pr-str chunks)]
             [:td [chord-chunks-com chunks]]
             [:td phrase]]))]])))

(defn chords-view [{:as args :keys [port-id]}]
  (when (not= port-id dummy-port-id)
    (let [port (get-port port-id)
          {:keys [*binary-chord-string]} port
          binary-chord-string @*binary-chord-string
          {:keys [chunks]} (when binary-chord-string
                             (parse-binary-chord-string binary-chord-string))]
      [:div {:class "pa3 pb0"}
       [:div.mb2
        (button #(query-all-chordmaps! port) ["Read Chords"] :size "small" :primary true)]
       [:div.f2.mb3
        [:div.dib "Last Chord: "]
        [chord-chunks-com chunks]]
       [chords-table port]])))
