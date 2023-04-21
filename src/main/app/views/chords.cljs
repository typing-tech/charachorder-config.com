(ns app.views.chords
  (:require [app.codes :refer [code-int->short-dom]]
            [app.components :refer [button concat-classes]]
            [app.components.phrase-editor :refer [phrase-editor]]
            [app.db :refer [*db]]
            [app.macros :refer-macros [->hash cond-xlet]]
            [app.serial.constants :refer [dummy-port-id get-port]]
            [app.serial.ops :as ops :refer [commit! delete-chord!
                                            query-all-chordmaps! read-chord! set-chord!]]
            [app.utils :refer [binary->hex hex-chord-string->sorted-chunks
                               hex-str->bin-str pad-left
                               parse-binary-chord-string phrase->chunks small-hex->decimal]]
            [datascript.core :refer [transact!]]
            [goog.string :refer [format]]
            [posh.reagent :as posh]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query-all-chords-button [port]
  (button #(query-all-chordmaps! port) ["Read Chords"]
          :size "small" :primary true
          :classes ["mr0"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn chord-chunks-com [hex-chord-string]
  (let [chunks (hex-chord-string->sorted-chunks hex-chord-string)]
    (into
     [:div {:class "chord-chunks"}]
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

(defn chords-table-row [port-id
                        active-hex-chord-string
                        {:keys [e chord-id hex-chord-string phrase]}]
  (let [{is-editing :chord/is-editing
         chord-index :chord/index}
        @(posh/pull *db '[:chord/is-editing
                          :chord/index] e)
        toggle-editing! #(posh/transact! *db [{:chord/id chord-id
                                               :chord/is-editing (not is-editing)}])
        delete! (fn []
                  (let [cb (fn []
                             (let [chord [:chord/id [port-id hex-chord-string]]]
                               (js/console.log "DELETED CHORD" (pr-str chord))
                               (transact! *db [[:db/retractEntity chord]])))]
                    (delete-chord! port-id hex-chord-string cb))
                  (commit! port-id))
        delete-button (button delete! ["Delete"] :size "xsmall" :danger true)
        apply-active-chord!
        (fn []
          (delete-chord! port-id hex-chord-string #())
          (let [cb (fn []
                     (transact! *db [[:db/add e :chord/hex-chord-string active-hex-chord-string]]))]
            (set-chord! port-id active-hex-chord-string phrase cb))
          (commit! port-id))
        apply-chord-button (button apply-active-chord!
                                   ["Use Active Chord"] :size "xsmall" :secondary true)]
    [:<>
     [:tr {:key e
           :on-click toggle-editing!
           :class (concat-classes "pointer"
                                  (when is-editing "editing"))}
      [:td
       (button toggle-editing! ["Edit"]
               :size "xsmall" :minimal true)]
      [:td [chord-chunks-com hex-chord-string]]
      [:td [phrase-chunks-com phrase]]]
     (when is-editing
       [:tr {:class "editing"}
        [:td delete-button]
        [:td apply-chord-button]
        [:td [phrase-editor port-id e hex-chord-string phrase]]])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-chords [port-id]
  (->> @(posh/q '[:find ?e ?chord-id ?index ?hex-chord-string ?phrase
                  :in $ ?port-id
                  :where
                  [?e :chord/port-id ?port-id]
                  [?e :chord/index ?index]
                  [?e :chord/id ?chord-id]
                  [?e :chord/hex-chord-string ?hex-chord-string]
                  [?e :chord/phrase ?phrase]]
                *db port-id)
       (map #(zipmap [:e :chord-id :index :hex-chord-string :phrase] %))
       (sort-by :index)))

(defn chords-table [{:as port :keys [port-id *is-reading-chords *chord-read-index *num-chords]}
                    active-hex-chord-string]
  (if @*is-reading-chords
    [:div (format "Reading Chords (%d/%d)" @*chord-read-index @*num-chords)]
    (let [chords (get-chords port-id)]
      ;; (js/console.log "re-rendering chords table")
      [:table {:class "pure-table pure-table-horizontal chords-table"}
       [:thead
        [:tr
         [:th [query-all-chords-button port]]
         [:th "Chord"]
         [:th "Phrase"]]]
       (into [:tbody] (map (fn [chord] [chords-table-row port-id active-hex-chord-string chord])
                           chords))])))

(defn chord-reader []
  (let [*focused (r/atom false)]
    (fn [port-id]
      (let [enter-chord-reading-mode! #(ops/set-param! port-id :enable-serial-debugging true)
            leave-chord-reading-mode! #(ops/set-param! port-id :enable-serial-debugging false)]
        [:input {:type "text"
                 :class (concat-classes "chord-reader")
                 :on-focus (fn []
                             (reset! *focused true)
                             (enter-chord-reading-mode!))
                 :on-blur (fn []
                            (reset! *focused false)
                            (leave-chord-reading-mode!))
                 :value ""
                 :on-change #()
                 :placeholder (if-not @*focused
                                "Click to focus here and chord set the active chord below."
                                "Unfocus or dot i/o Manager will break and your device will be slow.")}]))))

(defn chords-view [{:keys [port-id]}]
  (when (not= port-id dummy-port-id)
    (let [port (get-port port-id)
          {:keys [*binary-chord-string]} port
          active-hex-chord-string (when-let [bcs @*binary-chord-string]
                                    (binary->hex bcs))]
      [:div {:class "pb0"}
       [:div.pt3.pl3
        [:div.mb2.w-50 [chord-reader port-id]]
        [:div.pb2
         (button #()
                 ["New Chord"] :size "xsmall" :success true
                 :classes ["button-success" "v-top" "mr2"])
         [:div.f3.dib.mr3.color-secondary "Active Chord: "]
         (when active-hex-chord-string [chord-chunks-com active-hex-chord-string])
         [:div.dib.gray.f7.mh3 active-hex-chord-string]
         nil]]
       [:div {:class "chords-table-container"}
        [chords-table port active-hex-chord-string]]])))
