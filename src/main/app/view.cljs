(ns app.view
  (:require ["react-scroll-to-bottom" :as react-scroll-to-bottom]
            [app.components :refer [button concat-classes]]
            [app.csv :refer [on-drag-over! read-dropped-keymap-csv!]]
            [app.db :as db :refer [*db]]
            [app.macros :as mac :refer-macros [cond-xlet ->hash]]
            [app.ratoms :refer [*active-port-id *current-tab-view
                                *nav-expanded *num-devices-connected]]
            [app.serial :as serial :refer [has-web-serial-api?]]
            [app.serial.constants :refer [*ports dummy-port-id get-port]]
            [app.serial.ops :as ops]
            [app.utils :refer [human-time-with-seconds]]
            [app.views.chords :refer [chords-view]]
            [app.views.codes :refer [codes-view]]
            [app.views.keymap :refer [keymap-view]]
            [app.views.params :refer [params-view]]
            [app.views.resets :refer [resets-view]]
            [app.views.settings :refer [settings-view]]
            [goog.string :as gstring :refer [format]]
            [oops.core :refer [oapply oapply! oapply!+ oapply+ ocall ocall!
                               ocall!+ ocall+ oget oget+ oset! oset!+]]
            [posh.reagent :as posh :refer [pull q transact!]]
            [app.settings :as settings]))
(def ScrollToBottom react-scroll-to-bottom/default)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn footer-com []
  [:div {:class "footer tc mw-100 f6 pt3 pb3 light-purple"}
   "Disclaimer: This site is not affiliated, associated, authorized, endorsed by,
   or in any way officially connected with CharaChorder.
   The official CharaChorder website can be found at "
   [:a {:target "_blank" :href "https://www.charachorder.com/"}
    "https://www.charachorder.com/"] "."])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn disconnect-button [port-id]
  (button #(ops/disconnect! port-id) ["Disconnect"]
          :size "xsmall"
          :warning true))

(defn reboot-tool-button []
  (button #(oset! js/window "location" "?")
          ["Reboot"]
          :size "xsmall" :warning true :classes ["mr0"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn nav
  "The left nav bar."
  [{:as args :keys [port-id nav-expanded num-devices]}]
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

(defn sample-usages-com []
  [:<>
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
      [:li
       "You " [:a {:href "?cc1-layout="} "start the tool in CC1 read-only mode"] ","]
      [:li [:a {:href "?cc-lite-layout="} "or CC Lite mode."]]
      [:li "You drag and drop the default layout CSV to get started."]
      [:li "You make changes to the layout."]
      [:li "You share the layout via CSV or the URL."]]]

    [:div {:class "card card--li"}
     [:ol
      [:li "You arrive via a read-only mode link."]
      [:li "You make changes to the layout."]
      [:li "You share the layout via CSV or the URL."]]]]])

(defn no-device-main-view [_]
  [:div {:id "main" :class "pa3"}
   [:h1 "No Device Connected Yet"]
   [:p "Connect a device using the button on the left."]
   [sample-usages-com]

   [footer-com]])

(defn debug-buttons []
  [:<>
   (button #(transact! *db [[:db/add -1 :error/error "foo"]]) ["Add Dummy Error"]
           :classes ["button-xsmall" "ma2 ml6"]
           :primary false
           :danger true)])

(defn tab-menu [{:keys [port-id]}]
  (let [current @*current-tab-view
        {:keys [*device-name *device-version *num-chords]} (get-port port-id)
        gen-button (fn [tab label & {:keys [danger]}] 
                     (letfn [(f []
                                (settings/set! :last-view tab)
                                (reset! *current-tab-view tab))]
                       (button f [label]
                               :size "xsmall"
                               :active (= current tab)
                               :danger danger)))]
    [:div {:id "tab-menu"}
     (if (= port-id dummy-port-id)
       [:<>
        [switch-to-real-device-mode-button]]
       [:<>
        [:div {:class "device-string"}
         @*device-name [:br]
         @*device-version " - " @*num-chords " chords"]
        (gen-button :keymap "Key Map")
        (gen-button :chords "Chords")
        (gen-button :params "Parameters") 
        [:div.dib.ph2]
        (gen-button :resets "RESETs Toolbox")
        (gen-button :codes "Action Codes")
        [:div.dib.ph2]
        (gen-button :settings "Settings")
        [:div {:class "absolute top-0 right-0 h-100 flex items-center mr3"}
         [disconnect-button port-id]
         [reboot-tool-button]]])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn api-log-content [port-id]
  (let [{:keys [*api-log]} (get-port port-id)
        log (if-not *api-log {} (or @*api-log {}))
        ks (->> (keys log)
                (sort)
                (take-last 500))
        f
        (fn [index]
          (let [{:keys [stdin stdin-t stdout stdout-t]} (get log index)
                wait-time (- stdout-t stdin-t)]
            [:p {:key (str index)}
             [:span.time.mr3 (human-time-with-seconds stdin-t)]
             [:span.stdin.msg.mr4 stdin]
             [:span.stdout.msg stdout]]))
        els (map f ks)]
    (into [:<>] els)))

(defn api-log-view [{:keys [port-id]}]

  [:> ScrollToBottom {:class-name "log api-log"
                      :initial-scroll-behavior "smooth"}
   [api-log-content port-id]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn serial-log-content [port-id]
  (let [{:keys [*serial-log]} (get-port port-id)
        log (if-not *serial-log {} (or @*serial-log {}))
        ks (->> (keys log)
                (sort))
        f
        (fn [index]
          (let [{:keys [stdout stdout-t]} (get log index)]
            [:p {:key (str index)}
             [:span.time.mr3 (human-time-with-seconds stdout-t)]
             [:span.stdout.msg stdout]]))
        els (map f ks)]
    (into [:<>] els)))

(defn serial-log-view [{:keys [port-id]}]

  [:> ScrollToBottom {:class-name "log serial-log"
                      :initial-scroll-behavior "smooth"}
   [serial-log-content port-id]])

(defn logs-com [{:as args :keys [port-id]}]
  (when (not= port-id dummy-port-id)
    [:<>
     [api-log-view args]
     [serial-log-view args]]))

(defn main-view [{:as args :keys [port-id]}]
  (let [last-view :keymap]
    [:div {:id "main" :class ""}
    [tab-menu args]
    [:div {:id "viewport"}
     (let [tab-view (if (= port-id dummy-port-id)
                      :keymap
                      (or @*current-tab-view last-view))]
       (case tab-view
         :keymap [keymap-view args]
         :chords [chords-view args]
         :params [params-view args]
         :resets [resets-view args]
         :codes [codes-view args]
         :settings [settings-view args]))
     [footer-com]
     (comment [logs-com args])
     nil]]))

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
     (into [:<>] (map (fn [[e msg]] [error-modal e msg]) errors))]))

(defn no-web-serial-api-view []
  [:<>
   [:div {:class "pure-u-1 tc"}
    [:h1.blink "Your browser does not support the Web Serial API."]
    [:p "As of Janurary 2023, the only known browsers to support the Web Serial API is Chrome, Edge, and Opera from April 2021 onwards."]]

   [footer-com]])

(defn super-root-view []
  (cond
    (or false (not (has-web-serial-api?))) [no-web-serial-api-view]
    :else [root-view]))
