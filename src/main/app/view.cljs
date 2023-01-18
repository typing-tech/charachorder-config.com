(ns app.view
  (:require
   [clojure.string :as str]
   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [reagent.dom :as rdom]
   [posh.reagent :as posh :refer [transact! pull q]]
   [testdouble.cljs.csv :as csv]

   [app.macros :as mac :refer-macros [cond-xlet ->hash]]
   [app.ratoms :refer [*nav-expanded
                       *num-device-connected
                       *active-port-id
                       *current-tab-view]]
   [app.db :as db :refer [*db]]
   [app.components :refer [button concat-classes]]
   [app.serial :as serial :refer [has-web-serial-api?
                                  *ports]]
   [app.codes :refer [var-params]]
   [app.views.params :refer [params-view]]
   [app.views.keymap :refer [keymap-view]]
   [app.hw.cc1 :as cc1]))

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

(defn nav [{:as args :keys [nav-expanded num-devices]}]
  (let [xs (->> @*ports
                vals
                (sort-by :i))]
    [:div {:id "nav"
           :class (concat-classes "pure-u tc"
                                  (when-not nav-expanded "nav-collapsed"))}
     (if nav-expanded
       [:<>
        [:h1.f5.mb3.mt4 "CharaChorder Config"]

        (into [:div] (map (partial gen-device-buttons args) xs))

        (button serial/request! ["Connect New Device"]
                :primary (= 0 num-devices)
                :classes ["mt2 mb1"
                          (when (< 0 num-devices) "button-xsmall")])
        [:div.f6 num-devices " device(s) connected."]

        [:div {:class "absolute top-0 right-0 pointer"
               :on-click #(reset! *nav-expanded false)}
         [:div.dib.ma2 "X"]]]
       [:<>
        [menu-button #(reset! *nav-expanded true)]])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn no-device-main-view [_]
  [:div {:id "main" :class "pure-u-1 pa3"}
   [:h1 "No Device Connected Yet"]
   [:p "Connect a device using the button on the left."]])

(defn tab-menu []
  (let [current @*current-tab-view
        gen-button (fn [tab label]
                     (button #(reset! *current-tab-view tab) [label]
                             :classes ["button-xsmall" "ma2 mh2"]
                             :primary (= current tab)))]
    [:div {:id "tab-menu"}
     (gen-button :keymap "Key Map")
     (gen-button :params "Parameters")]))

(defn main-view [{:as args :keys [port-id]}]
  [:div {:id "main" :class "pure-u-1"}
   [tab-menu]
   (let [tab-view (or @*current-tab-view :params)]
     (case tab-view
       :keymap [keymap-view args]
       :params [params-view args]))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn load-csv-text! [port-id csv]
  (let [xs (csv/read-csv csv)
        txs (mapv (fn [[layer location code]]
                    (let [switch-key-id (get cc1/location->switch-key-id location)
                          attr-ns (str layer "." switch-key-id)
                          attr (keyword attr-ns "code")]
                      [:db/add [:port/id port-id] attr code]))
                  xs)]
    (transact! *db txs)))

(defn on-drag-over! [e]
  (.preventDefault e)
  (.stopPropagation e)
  (oset! e "dataTransfer.dropEffect" "copy"))

(defn read-keymap-csv! [port-id
                        ^js e]
  (.preventDefault e)
  (.stopPropagation e)
  (let [files (oget e "dataTransfer.files")
        file (first files)]
    (when (and file (str/ends-with? (str/lower-case (oget file "name"))
                                    ".csv"))
      (js/console.log file)
      (let [fr (new js/FileReader)]
        (.addEventListener fr "load" #(load-csv-text! port-id (oget fr "result")) false)
        (.readAsText fr file)))))

(defn root-view []
  (let [active-port-id @*active-port-id
        num-devices @*num-device-connected
        nav-expanded (or @*nav-expanded
                         (= num-devices 0))
        port-id active-port-id
        args (->hash num-devices nav-expanded active-port-id port-id)]
    [:div {:id "root"
           :on-drop #(read-keymap-csv! port-id %)
           :on-drag-over on-drag-over!
           :class (concat-classes "pure-g"
                                  (when-not nav-expanded "nav-collapsed"))}
     [nav args]
     (if-not active-port-id
       [no-device-main-view args]
       [main-view args])]))

(defn super-root-view []
  (cond
    (not (has-web-serial-api?)) [no-web-serial-api-view]
    :else [root-view]))
