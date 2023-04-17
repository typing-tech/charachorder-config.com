(ns app.views.chords 
  (:require [app.codes :refer [code-int->short-dom]]
            [app.components :refer [button concat-classes]]
            [app.db :refer [*db]]
            [app.macros :refer-macros [->hash cond-xlet]]
            [app.serial.constants :refer [dummy-port-id get-port]]
            [app.serial.ops :refer [del-chord! query-all-chordmaps!]]
            [app.utils :refer [hex-str->bin-str parse-binary-chord-string
                               phrase->chunks small-hex->decimal]]
            [goog.string :refer [format]]
            [posh.reagent :as posh]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn chord-chunks-com [hex-chord-string]
  (let [bcs (if (= 128 (count hex-chord-string))
              hex-chord-string
              (hex-str->bin-str hex-chord-string))
        chunks (->> bcs
                    parse-binary-chord-string
                    :chunks
                    (filter #(not= 0 %)))]
    (into
     [:div {:class "chord-chunks mh2"}]
     (for [chunk chunks]
       (-> (code-int->short-dom chunk)
           (assoc-in [1 :class] "chord-chunks__chunk"))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn decorate-phrase-chunk [v]
  (let [[_tag _m x] v]
    (if (= 1 (count x))
      v
      (update-in v [1 :class]
                 #(concat-classes % "phrase__chunk--complex")))))

(defn phrase-chunks-com [phrase]
  (assert (string? phrase))
  (let [xf (comp (map #(code-int->short-dom %))
                 (map decorate-phrase-chunk))
        chunks (phrase->chunks phrase)
        actions (->> chunks
                 (into [] xf))]
    (into [:div {:class "phrase"
                 :data-phrase phrase
                 :data-chunks (pr-str chunks)}]
          actions)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
         [:th "Ops"]
         [:th "Chord"]
         [:th "Phrase"]]]
       [:tbody
        (for [{:keys [e index hex-chord-string phrase]} chords]
          [:tr {:key e}
           [:td index]
           [:td
            ;; (button #(del-chord! port-id hex-chord-string)
            ;;         ["Delete"] :size "xsmall" :danger true)
            nil]
           [:td [chord-chunks-com hex-chord-string]]
           [:td [phrase-chunks-com phrase]]])]])))

(defn chords-view [{:as args :keys [port-id]}]
  (when (not= port-id dummy-port-id)
    (let [port (get-port port-id)
          {:keys [*binary-chord-string]} port]
      [:div {:class "pa3 pb0"}
       [:div.mb2
        (button #(query-all-chordmaps! port) ["Read Chords"] :size "small" :primary true)]
       [:div.f2.mb3
        [:div.dib "Last Chord: "]
        (when-let [bcs @*binary-chord-string]
          [chord-chunks-com bcs])]
       [chords-table port]])))
