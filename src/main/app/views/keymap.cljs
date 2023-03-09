(ns app.views.keymap
  (:require
   ; ["react-double-marquee" :as react-double-marquee]
   [goog.object]
   [goog.string.format]
   [goog.string :as gstring :refer [format]]

   [cljs.core.async :as async
    :refer [chan <! >! onto-chan! close! put!]
    :refer-macros [go go-loop]]
   [clojure.string :as str]
   [reagent.core :as r]
   [posh.reagent :as posh :refer [transact!]]
   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]

   [app.macros :as mac]
   [app.components :refer [button popover concat-classes]]
   [app.db :as db :refer [*db]]
   [app.codes :refer [keymap-codes
                      code->keymap-code
                      longest-action-text

                      ascii-keymap-codes
                      cp1252-keymap-codes
                      keyboard-keymap-codes
                      mouse-keymap-codes
                      charachorder-keymap-codes
                      charachorder-one-keymap-codes
                      raw-keymap-codes]]
   [app.preds :refer [is-device-not-yet-determined?
                      is-device-cc1?
                      is-device-cc-lite?]]
   [app.hw :refer [get-hw-switch-keys
                   get-hw-location->switch-key-id]]
   [app.csv :refer [download-csv! update-url-from-db!]]
   [app.serial.constants :refer [get-port dummy-port-id]]
   [app.serial.ops :refer [set-keymap!
                           commit!
                           refresh-keymaps-after-commit!]]))
; (def Marquee (oget react-double-marquee "default"))

(defonce *tab (r/atom :ascii))

(defn db-set! [port-id a v]
  (transact! *db [[:db/add [:port/id port-id] a v]]))

(defn code-tooltip [{:keys [code type action action-desc notes]}]
  [:table {:class "pure-table pure-table-horizontal pure-table-striped measure-wide"}
   [:tbody
    [:tr [:td.tr "Code"] [:td code]]
    [:tr [:td.tr "Type"] [:td type]]
    [:tr [:td.tr "Action"] [:td action]]
    [:tr [:td.tr "Desc"] [:td action-desc]]
    (when-not (str/blank? notes)
      [:tr [:td.tr "Notes"] [:td notes]])]])

(defn action-chooser-popover []
  (let [*keymap-code (r/atom nil)
        gen-button (fn [k label]
                     (button #(reset! *tab k) [label] :classes ["button-xsmall"]
                             :primary (= @*tab k)))]
    (fn [port-id layer switch-key-id key-ns]
      [:div {:class "action-chooser-popover"}
       [:div {:class "action-chooser-popover__tab"}
        (gen-button :ascii "ASCII")
        (gen-button :cp-1252 "CP-1252")
        (gen-button :keyboard "Keyboard")
        (gen-button :mouse "Mouse")
        (gen-button :charachorder "CharaChorder")
        (gen-button :charachorder-one "CharaChorder One")
        (gen-button :raw "Raw Scancode")
        (button #(db-set! port-id (keyword key-ns "editing") false)
                ["X"] :classes ["button-xsmall" "fr" "close-button ma0 mr0"] :error true)]

       (let [{:as keymap :keys [code action-desc notes]} (get code->keymap-code @*keymap-code)]
         [:div {:class "action-chooser-popover__info mv2 pa2"}
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
                       :on-click
                       (fn []
                         (db-set! port-id (keyword key-ns "code") code)
                         (db-set! port-id (keyword key-ns "editing") false)
                         (update-url-from-db! port-id)
                         (when (not= port-id dummy-port-id)
                           (set-keymap! port-id layer switch-key-id code)))}
                  action]))
             gen-row (fn [xs] (into [:tr] (map gen-code xs)))]
         [:table {:class "action-chooser-popover__codes mt3"}
          (into [:tbody] (map gen-row codes))])])))

(defn action-chooser-com []
  (let [*hovered (r/atom false)]
    (fn [port-id layer switch-key]
      (let [key-ns (str layer "." switch-key)
            open-key (keyword key-ns "editing")
            code-key (keyword key-ns "code")
            hw-code-key (keyword key-ns "hw.code")
            m @(posh/pull *db [open-key code-key hw-code-key] [:port/id port-id])
            is-open (-> m open-key boolean)
            code (-> m code-key)
            hw-code (-> m hw-code-key)
            {:as keymap-code :keys [action]} (get code->keymap-code code)]
        (popover
         {:isOpen (or @*hovered is-open)
          :positions ["bottom" "top" "right" "left"]
          :align "start"
          :reposition true
          :content (if (and (not is-open) @*hovered)
                     (r/as-element [code-tooltip keymap-code])
                     (r/as-element [action-chooser-popover port-id layer switch-key key-ns]))}
         [:div {:class "action-chooser__action"
                :on-mouse-enter #(reset! *hovered true)
                :on-mouse-leave #(reset! *hovered false)
                :on-click #(db-set! port-id open-key true)}
          (if-not (str/blank? action)
            [:div {:class (concat-classes (when (and (not= hw-code code)
                                                     (not= port-id dummy-port-id)) "yellow"))}
             action]
            [:div.gray (gstring/unescapeEntities "&nbsp;")])])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cc1-stick-key [{:keys [port-id]} switch-key]
  [:<>
   [:div.action-chooser
    [:div.action-chooser__layer "1"]
    [action-chooser-com port-id "A1" switch-key]]
   [:div.action-chooser
    [:div.action-chooser__layer "2"]
    [action-chooser-com port-id "A2" switch-key]]
   [:div.action-chooser
    [:div.action-chooser__layer "3"]
    [action-chooser-com port-id "A3" switch-key]]])

(defn cc1-stick [args switch-key-prefix]
  (let [td (fn [dir]
             [:td (when dir [cc1-stick-key args (str switch-key-prefix dir)])])]
    [:td
     [:table {:class "cc1"}
      [:tbody
       [:tr (td nil) (td "n")]
       [:tr (td "w") (td "d") (td "e")]
       [:tr (td nil) (td "s")]]]]))

(defn cc1-keymap-view [{:as args :keys [port-id]}]
  (let [commit-and-refresh!
        (fn []
          (commit! port-id)
          (refresh-keymaps-after-commit! port-id))]
    [:table {:class "cc1"}
     [:tbody
      [:tr
       [cc1-stick args "lp0"]
       [cc1-stick args "lr0"]
       [cc1-stick args "lm0"]
       [cc1-stick args "li0"]
       [cc1-stick args "lt0"]

       [cc1-stick args "rt0"]
       [cc1-stick args "ri0"]
       [cc1-stick args "rm0"]
       [cc1-stick args "rr0"]
       [cc1-stick args "rp0"]]
      [:tr
       [:td]
       [cc1-stick args "lr1"]
       [cc1-stick args "lm1"]
       [:td]
       [cc1-stick args "lt1"]
       [cc1-stick args "rt1"]
       [:td]
       [cc1-stick args "rm1"]
       [cc1-stick args "rr1"]]
      [:tr
       [:td]
       [:td
        (when (not= port-id dummy-port-id)
          [:div.cc1-cell-mw.tr.fr
           [:span.pink "WARNING: "]
           [:span "Do not excessively use COMMIT."]])]
       [:td.tc (when (not= port-id dummy-port-id)
                 (button #(commit-and-refresh!)
                         ["COMMIT"]
                         :primary true :danger true :size "small" :classes ["mr0"]))]
       [:td
        (when (not= port-id dummy-port-id)
          [:div.cc1-cell-mw
           [:span "A CC device is only guaranteed at least 10,000 commits per lifetime of the device."]])]

       [cc1-stick args "lt2"]
       [cc1-stick args "rt2"]
       [:td]
       [:td.tc (button #(download-csv! port-id)
                       ["Download" [:br] "Layout as CSV"]
                       :primary true :size "small" :classes ["mr0"])]]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:dynamic *cc-lite-debug* false)

(defn cc-lite-key [{:keys [port-id switch-keys selected-layer]}
                   key-id]
  (let [{:keys [location u]} (get switch-keys key-id)
        u (or u 1)]
    [:div {:class "cc-lite-key"
           :data-ccos-location-number location
           :style {:width (format "calc(%f * var(--cc-lite-key-base-width))" u)}}
     ; (js/console.log port-id selected-layer key-id)
     [action-chooser-com port-id selected-layer key-id]]))

(defn cc-lite-keymap-view [{:as args :keys [port-id]}]
  (let [port (get-port port-id)
        switch-keys (get-hw-switch-keys port)
        location->switch-key-id (get-hw-location->switch-key-id port)

        selected-layer-key (keyword port-id "selected-layer")
        m @(posh/pull *db [selected-layer-key] [:port/id port-id])

        selected-layer (or (get m selected-layer-key) "A1")

        set-selected-layer!
        (fn [x]
          (transact! *db [[:db/add [:port/id port-id] selected-layer-key x]]))
        commit-and-refresh!
        (fn []
          (commit! port-id)
          (refresh-keymaps-after-commit! port-id))

        args (mac/args switch-keys selected-layer)]
    [:<>
     [:div {:class "mv3 tc"}
      (button #(set-selected-layer! "A1") ["Primary Layer (A1)"] :active (= selected-layer "A1"))
      (button #(set-selected-layer! "A2") ["Fn Layer (A2)"] :active (= selected-layer "A2"))
      (button #(set-selected-layer! "A3") ["Tertiary Layer (A3)"] :active (= selected-layer "A3"))]
     [:div {:class "ph5"}
      (into
       [:div]
       (for [loc-num (range 53 (inc 66))]
         [cc-lite-key args (get location->switch-key-id (str loc-num))]))
      (into
       [:div]
       (for [loc-num (range 39 (inc 52))]
         [cc-lite-key args (get location->switch-key-id (str loc-num))]))
      (into
       [:div]
       (for [loc-num (range 26 (inc 38))]
         [cc-lite-key args (get location->switch-key-id (str loc-num))]))
      (into
       [:div]
       (for [loc-num (range 12 (inc 25))]
         [cc-lite-key args (get location->switch-key-id (str loc-num))]))
      (into
       [:div]
       (for [loc-num (range 0 (inc 11))]
         [cc-lite-key args (get location->switch-key-id (str loc-num))]))
      [:div.mb4]
      [:div.dib.w-50.tc.v-top
       (when (or *cc-lite-debug* (not= port-id dummy-port-id))
         [:<>
          [:div.dib.w-40.v-top
           [:span.pink "WARNING: "]
           [:span "Do not excessively use COMMIT."]]
          [:div.dib.w-20.v-top
           (button #(commit-and-refresh!)
                   ["COMMIT"]
                   :primary true :danger true :size "small" :classes ["mr0"])]
          [:div.dib.w-40.v-top
           [:span "A CC device is only guaranteed at least 10,000 commits per lifetime of the device."]]])]
      [:div.dib.w-50.tc.v-top
       (button #(download-csv! port-id)
               ["Download" [:br] "Layout as CSV"]
               :primary true :size "small" :classes ["mr0"])]]]))

(defn unsupported-keymap-view [_args]
  [:h1.mv5.tc.red "There is no keymap support for this device yet."])

(defn keymap-view [{:as args :keys [port-id]}]
  (let [port (get-port port-id)]
    [:<>
     [:div.mv2.tc.light-purple
      [:p.lh-solid "Did you know you can drag and drop a CSV here? And share the URL once it changes?"]
      [:p.lh-solid "A yellow action means that the action has not been COMMITted."]
      [:p.lh-solid "Action changes immediately take effect, but are not COMMITted."]]
     (cond
       ; true [unsupported-keymap-view args]
       (is-device-not-yet-determined? port) [:div]
       (is-device-cc1? port) [cc1-keymap-view args]
       (is-device-cc-lite? port) [cc-lite-keymap-view args]
       :else [unsupported-keymap-view args])]))
