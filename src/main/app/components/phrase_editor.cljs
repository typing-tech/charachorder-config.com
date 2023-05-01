(ns app.components.phrase-editor
  (:require ["@tinymce/tinymce-react" :refer [Editor]]
            [app.codes :refer [code-int->keymap-code code-str->keymap-code
                               phrase-keymap-codes typeable-keymap-codes]]
            [app.components :refer [concat-classes]]
            [app.db :refer [*db]]
            [app.macros :refer-macros [->hash cond-xlet]]
            [app.serial.ops :refer [commit! set-chord!]]
            [app.utils :refer [chunks->phrase phrase->chunks]]
            [clojure.string :as str]
            [datascript.core :refer [transact!]]
            [goog.string :as gstring :refer [format]]
            [oops.core :refer [oget]]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def editor-init
  {:inline true
   :menubar false
   :toolbar false
   :paste_remove_styles_if_webkit false
   :smart_paste false
   :paste_as_text true
   :paste_block_drop false
   :contextmenu ["help"]
   :valid_elements "inline[*]"
   :plugins ["help"]})

(defn chunk->span-html [code-int]
  (let [{:keys [code action action-desc notes]} (get code-int->keymap-code code-int)
        inner-html action
        chunk-class (if (= 1 (count inner-html))
                      ""
                      "complex-chunk")]
    (format "<inline data-code=\"%s\" class=\"%s\" tabindex=\"-1\" contenteditable=\"false\">%s</inline>"
            code chunk-class inner-html)))

(defn html->chunks [html]
  (let [xs (re-seq #"data-code=\"(\d+)\"" html)]
    (mapv #(js/parseInt (second %)) xs)))

(defn action-code-row []
  (let [*hovered (r/atom false)]
    (fn [*editor
         {:keys [code code-int action action-desc notes]}]
      (let [^js editor @*editor]
        [:div {:key code :class (concat-classes "pointer"
                                                (when @*hovered "bg-dark-blue"))
               :on-mouse-enter #(reset! *hovered true)
               :on-mouse-leave #(reset! *hovered false)
               :on-mouse-down (fn [e]
                                (.preventDefault e)
                                (let [html (chunk->span-html code-int)]
                                  (js/console.log html)
                                  (.insertContent editor html)))
               :on-mouse-up (fn [e] (.preventDefault e))}
         [:span.code {:title notes}
          code
          (gstring/unescapeEntities "&nbsp;") (gstring/unescapeEntities "&nbsp;")
          [:span.white action]
          (when-not (str/blank? action-desc)
            [:<> " - " action-desc])]]))))

(defn on-editor-change! [value editor *html]
  ;; (js/console.log value)
  (reset! *html value))

(defn on-before-add-undo! [e editor]
  nil)

(def nav-keys #{"ArrowLeft" "ArrowRight" "ArrowUp" "ArrowDown" "Backspace"})

(defn on-key-down! [^js e
                    ^js editor
                    update-phrase!]
  (when-not (nav-keys (oget e "key"))
    (.preventDefault e))
  (let [key (oget e "key")
        {:as m :keys [code-int]} (get typeable-keymap-codes key)] 
    (cond
      (= key "Enter") (update-phrase!)
      (nav-keys key) nil
      (oget e "ctrlKey") nil
      m
      (let [html (chunk->span-html code-int)
            ^js sel (oget editor "selection")]
        (.setContent sel html))
      :else (do nil
                ;; (js/console.log e)
                nil))))

(defn phrase-editor []
  (let [*editor (r/atom nil)
        *html (r/atom nil)]
    (fn [port-id e hex-chord-string phrase]
      (let [initial-chunks (phrase->chunks phrase)
            initial-html (->> (map chunk->span-html initial-chunks)
                              (apply str))
            html-chunks (when-let [html @*html]
                          (html->chunks html))

            update-phrase!
            (fn []
              (let [phrase (chunks->phrase html-chunks)
                    cb (fn []
                         (transact! *db [[:db/add e :chord/phrase phrase]]))]
                ;; (js/console.log (pr-str phrase))
                (set-chord! port-id hex-chord-string phrase cb)
                (commit! port-id)))]
        (when (nil? @*html) (reset! *html initial-html))
        [:<>
         [:div
          [:span {:class "light-yellow mr2"} "Press 'Enter' to save."]
          [:span {:class "code f7 gray mb2"} (pr-str html-chunks)]]
         [:div {:class "phrase-editor__container"}
          [:> Editor {:tinymce-script-src "/tinymce/tinymce.min.js"
                      :on-init (fn [_e editor]
                                 (reset! *editor editor))
                      :on-editor-change #(on-editor-change! %1 %2 *html)
                      :on-before-add-undo #(on-before-add-undo! %1 %2)
                      :on-key-down #(on-key-down! %1 %2 update-phrase!)
                      :value @*html
                      :init (clj->js editor-init)}]]
         [:div {:class "phrase-editor__actions"}
          [:h5.tc "Insert Special Action Codes"]
          (map (fn [{:as m :keys [code]}]
                 ^{:key code} [action-code-row *editor m])
               phrase-keymap-codes)]]))))
