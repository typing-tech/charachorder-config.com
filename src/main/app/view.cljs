(ns app.view
  (:require
   ["react-scroll-to-bottom" :as react-scroll-to-bottom]
   [goog.string :as gstring :refer [format]]

   [clojure.string :as str]
   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [reagent.dom :as rdom]
   [posh.reagent :as posh :refer [transact! pull q]]

   [app.macros :as mac :refer-macros [cond-xlet ->hash]]
   [app.ratoms :refer [*nav-expanded
                       *num-devices-connected
                       *active-port-id
                       *current-tab-view]]
   [app.db :as db :refer [*db]]
   [app.components :refer [button concat-classes]]
   [app.serial.constants :refer [*ports dummy-port-id get-port]]
   [app.serial :as serial :refer [has-web-serial-api?]]
   [app.views.params :refer [params-view]]
   [app.views.keymap :refer [keymap-view]]
   [app.views.resets :refer [resets-view]]
   [app.csv :refer [on-drag-over! read-dropped-keymap-csv!]]))
(def ScrollToBottom react-scroll-to-bottom/default)

(defn no-web-serial-api-view []
  [:div {:class "pure-u-1 tc"}
   [:h1.blink "Your browser does not support the Web Serial API."]
   [:p "As of Janurary 2023, the only known browsers to support the Web Serial API is Chrome, Edge, and Opera from April 2021 onwards."]])

(defn gen-device-buttons [{:keys [active-port-id]}
                          {:keys [port-id *device-name]}]
  (let [f (fn [] (reset! *active-port-id port-id))]
    (button f [[:span.f6 @*device-name]]
            :classes ["mb2"
                      (when (= port-id active-port-id) "pure-button-primary")])))

(defn menu-button [f]
  [:div.menu-button {:on-click f} [:div]])

(defn switch-to-real-device-mode-button []
  (button #(oset! js/window "location" "?")
          ["Switch to Real Device Mode"]
          :size "xsmall" :warning true :classes ["mr0"]))

(defn reboot-tool-button []
  (button #(oset! js/window "location" "?")
          ["Reboot Tool"]
          :size "xsmall" :warning true :classes ["mr0"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn nav [{:as args :keys [port-id nav-expanded num-devices]}]
  (let [xs (->> @*ports
                vals
                (sort-by :i))
        close-button
        [:div {:class "absolute top-0 right-0 pointer"
               :on-click #(reset! *nav-expanded false)}
         [:div.dib.ma2 "X"]]]
    [:div {:id "nav"
           :class (concat-classes "pure-u tc"
                                  (when-not nav-expanded "nav-collapsed"))}
     (cond
       (not nav-expanded)
       [:<>
        [menu-button #(reset! *nav-expanded true)]]

       (= port-id dummy-port-id)
       [:<>
        [:h1.f5.mb3.mt4 "CharaChorder Config"]
        [switch-to-real-device-mode-button]
        close-button]

       :else
       [:<>
        [:h1.f5.mb3.mt4 "CharaChorder Config"]

        (into [:div] (map (partial gen-device-buttons args) xs))

        (button serial/request! ["Connect New Device"]
                :primary (= 0 num-devices)
                :classes ["mt2 mb1"
                          (when (< 0 num-devices) "button-xsmall")])
        [:div.f6 num-devices " device(s) connected."]

        close-button])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn no-device-main-view [_]
  [:div {:id "main" :class "pure-u-1 pa3"}
   [:h1 "No Device Connected Yet"]
   [:p "Connect a device using the button on the left."]

   [:h1.mt6.mb2 "Samples Usages"]
   [:div {:class "flex flex-wrap"}
    [:div {:class "card card--li"}
     [:ol
      [:li "Connect the device."]
      [:li "Manually modify the keymap."]
      [:li "COMMIT"]]]
    [:div {:class "card card--li"}
     [:ol
      [:li "Connect the device."]
      [:li "Manually modify the keymap."]
      [:li "The actions are changed when you use the device, but are " [:span.pink "LOST"] " upon disconnect of the device."]]]
    [:div {:class "card card--li"}
     [:ol
      [:li "You are happy with the current keymap of the device."]
      [:li "Connect the device."]
      [:li "You download the layout as a CSV for backup."]
      [:li "You share the layout via the URL."]]]
    [:div {:class "card card--li"}
     [:ol
      [:li "Connect the device."]
      [:li "Drag and drop another person's CSV file in the center of this window."]
      [:li "COMMIT"]]]
    [:div {:class "card card--li"}
     [:ol
      [:li "Connect the device."]
      [:li "Drag and drop another person's CSV file in the center of this window."]
      [:li "You don't like it, so you drag and drop your backup layout CSV to restore previous behavior."]]]

    [:div {:class "card card--li"}
     [:ol
      [:li "You " [:a {:href "?cc1-layout="} "start the tool in read-only mode"] "."]
      [:li "You drag and drop the default layout CSV to get started."]
      [:li "You make changes to the layout."]
      [:li "You share the layout via CSV or the URL."]]]

    [:div {:class "card card--li"}
     [:ol
      [:li "You arrive via a read-only mode link."]
      [:li "You make changes to the layout."]
      [:li "You share the layout via CSV or the URL."]]]]])

(defn debug-buttons []
  [:<>
   (button #(transact! *db [[:db/add -1 :error/error "foo"]]) ["Add Dummy Error"]
           :classes ["button-xsmall" "ma2 ml6"]
           :primary false
           :danger true)])

(defn tab-menu [{:keys [port-id]}]
  (let [current @*current-tab-view
        {:keys [*device-name *device-version]} (get-port port-id)
        gen-button (fn [tab label & {:keys [danger]}]
                     (button #(reset! *current-tab-view tab) [label]
                             :size "xsmall"
                             :primary (= current tab)
                             :danger danger))]
    [:div {:id "tab-menu"}
     (if (= port-id dummy-port-id)
       [:<>
        [switch-to-real-device-mode-button]]
       [:<>
        [:div {:class "device-string"}
         (format "%s - %s" @*device-name @*device-version)]
        (gen-button :keymap "Key Map")
        (gen-button :params "Parameters")
        (gen-button :resets "RESETS Toolbox" :danger true)
        [:div {:class "absolute top-0 right-0 h-100 flex items-center mr3"}
         [reboot-tool-button]]])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn console-content [port-id]
  (let [{:keys [*console]} (get-port port-id)
        lines (if-not *console
                []
                (or @*console []))
        lines (take-last 100 lines)
        els (map (fn [[id t msg]]
                   [:p {:key (str id)} [:span.time t] [:span.msg msg]])
                 lines)]
    (into [:<>] els)))

(defn console-view [{:keys [port-id]}]

  [:> ScrollToBottom {:class-name "console"
                      :initial-scroll-behavior "smooth"}
   [console-content port-id]])

(defn main-view [{:as args :keys [port-id]}]
  [:div {:id "main" :class "pure-u-1"}
   [tab-menu args]
   [:div {:id "viewport"}
    (let [tab-view (or @*current-tab-view :params)]
      (case tab-view
        :keymap [keymap-view args]
        :params [params-view args]
        :resets [resets-view args]))]

   (when (not= port-id dummy-port-id)
     [console-view args])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn error-modal [e msg]
  (let [f #(transact! *db [[:db.fn/retractEntity e]])]
    [:div {:class "modal" :on-click f}
     [:div {:class "modal__content"}
      [:p msg]
      [:div {:class "absolute top-0 right-0 pointer red f3"
             :on-click f}
       [:div.dib.ma2 "X"]]]]))

(defn root-view []
  (let [active-port-id @*active-port-id
        num-devices @*num-devices-connected
        nav-expanded (or @*nav-expanded
                         (= num-devices 0))
        port-id active-port-id
        errors @(q '[:in $
                     :find ?e ?msg
                     :where
                     [?e :error/error ?msg]]
                   *db)
        args (->hash num-devices nav-expanded active-port-id port-id)]
    [:div {:id "root"
           :on-drop #(read-dropped-keymap-csv! port-id %)
           :on-drag-over on-drag-over!
           :class (concat-classes "pure-g"
                                  (when-not nav-expanded "nav-collapsed"))}
     [nav args]
     (if-not active-port-id
       [no-device-main-view args]
       [main-view args])
     (into [:<>]
           (map (fn [[e msg]]
                  [error-modal e msg])
                errors))]))

(defn super-root-view []
  (cond
    (not (has-web-serial-api?)) [no-web-serial-api-view]
    :else [root-view]))
