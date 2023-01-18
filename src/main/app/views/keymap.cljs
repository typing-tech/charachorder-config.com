(ns app.views.keymap
  (:require
   [goog.object]
   [goog.string.format]
   [goog.string :as gstring :refer [format]]

   [reagent.core :as r]
   [posh.reagent :as posh :refer [transact!]]

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

(defn db-set! [port-id a v]
  (transact! *db [[:db/add [:port/id port-id] a v]]))

(defn code-table []
  (let [*keymap-code (r/atom nil)
        gen-button (fn [k label]
                     (button #(reset! *tab k) [label] :classes ["button-xsmall"]
                             :primary (= @*tab k)))]
    (fn [port-id switch-key]
      [:div {:class "keymap-codes"}
       [:div {:class "keycode-codes__tab"}
        (gen-button :ascii "ASCII")
        (gen-button :cp-1252 "CP-1252")
        (gen-button :keyboard "Keyboard")
        (gen-button :mouse "Mouse")
        (gen-button :charachorder "CharaChorder")
        (gen-button :charachorder-one "CharaChorder One")
        (gen-button :raw "Raw Scancode")
        (button #(db-set! port-id (keyword switch-key "editing") false)
                ["X"] :classes ["button-xsmall" "fr" "close-button ma0 mr0"] :error true)]

       (let [{:as keymap :keys [code action-desc notes]} (get code->keymap-code @*keymap-code)]
         [:div {:class "keycode-codes__info mv2 pa2"}
          (when keymap [:div (format "%s - %s" code action-desc)])
          (when notes [:div notes])])

       (let [codes
             (case @*tab
               :ascii ascii-keymap-codes
               :cp-1252 cp1252-keymap-codes
               :keyboard keyboard-keymap-codes
               :mouse mouse-keymap-codes
               :charachorder charachorder-keymap-codes
               :charachorder-one charachorder-one-keymap-codes
               :raw raw-keymap-codes)
             gen-code
             (fn [{:keys [code action]}]
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
                       :on-click (fn []
                                   (db-set! port-id (keyword switch-key "code") code)
                                   (db-set! port-id (keyword switch-key "editing") false))}
                  action]))
             gen-row (fn [xs] (into [:tr] (map gen-code xs)))]
         [:table {:class "keycode-codes__codes mt3"}
          (into [:tbody] (map gen-row codes))])])))

(defn code-chooser-com [port-id switch-key]
  (let [open-key (keyword switch-key "editing")
        code-key (keyword switch-key "code")
        m @(posh/pull *db [open-key code-key] [:port/id port-id])
        is-open (-> m open-key boolean)
        code (-> m code-key)]
    (popover
     {:isOpen is-open
      :positions ["top" "bottom" "right" "left"]
      :align "start"
      :reposition true
      :content (r/as-element [code-table port-id switch-key])}
     [:div {:class "pointer" :on-click #(db-set! port-id open-key true)}
      (or code "NIL")])))

(defn keymap-view [{:keys [port-id]}]
  (let []
    [code-chooser-com port-id "L-0-0"]))
