(ns app.views.keymap
  (:require
   [goog.object]
   [goog.string.format]
   [goog.string :as gstring :refer [format]]
   [reagent.core :as r]
   [app.components :refer [button popover]]
   [app.db :as db :refer [*db]]
   [app.codes :refer [keymap-codes
                      code->keymap-code
                      ascii-keymap-codes
                      cp1252-keymap-codes
                      keyboard-keymap-codes
                      mouse-keymap-codes
                      charachorder-keymap-codes
                      charachorder-one-keymap-codes
                      raw-keymap-codes]]))

(defonce *tab (r/atom :ascii))

(defn code-table []
  (let [*keymap-code (r/atom nil)
        gen-button (fn [k label]
                     (button #(reset! *tab k) [label] :classes ["button-xsmall"]
                             :primary (= @*tab k)))
        gen-code (fn [{:keys [code action]}]
                   (let [tab @*tab
                         col-span (case tab
                                    :keyboard 1
                                    :mouse 1
                                    :charachorder 1
                                    :charachorder-one 1
                                    :raw 1
                                    (int (* 0.75 (count action))))]
                     [:td {:colSpan (str col-span)
                           :class (when (= tab :raw) "small")
                           :on-mouse-enter #(reset! *keymap-code code)
                           :on-mouse-leave #(reset! *keymap-code nil)
                           :on-click #()}
                      action]))
        gen-row (fn [xs]
                  (into [:tr] (map gen-code xs)))]
    (fn [port-id]
      [:div {:class "keymap-codes"}
       [:div {:class "keycode-codes__tab"}
        (gen-button :ascii "ASCII")
        (gen-button :cp-1252 "CP-1252")
        (gen-button :keyboard "Keyboard")
        (gen-button :mouse "Mouse")
        (gen-button :charachorder "CharaChorder")
        (gen-button :charachorder-one "CharaChorder One")
        (gen-button :raw "Raw Scancode")]

       (let [{:as keymap :keys [code action-desc notes]} (get code->keymap-code @*keymap-code)]
         [:div {:class "keycode-codes__info mv2 pa2"}
          (when keymap [:div (format "%s - %s" code action-desc)])
          (when notes [:div notes])])

       (let [codes (case @*tab
                     :ascii ascii-keymap-codes
                     :cp-1252 cp1252-keymap-codes
                     :keyboard keyboard-keymap-codes
                     :mouse mouse-keymap-codes
                     :charachorder charachorder-keymap-codes
                     :charachorder-one charachorder-one-keymap-codes
                     :raw raw-keymap-codes)]
         [:table {:class "keycode-codes__codes mt3"}
          (into [:tbody] (map gen-row codes))])])))

(defn code-chooser-com []
  (let []
    (fn [port-id]
      (let []
        (popover
         {:isOpen true
          :positions ["top" "bottom" "right" "left"]
          :align "start"
          :reposition true
          :content (r/as-element [code-table port-id])}
         [:div {:class "pointer"} "click"])))))

(defn keymap-view [{:keys [port-id]}]
  (let []
    [code-chooser-com port-id]))
