(ns app.core
  (:require
   [clojure.spec.alpha :as s]
   [orchestra-cljs.spec.test :as ost]
   [expound.alpha :as expound]
   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [promesa.core :as p]

   [reagent.dom :as rdom]
   [posh.reagent :as posh :refer [pull q]]

   [app.macros :as mac :refer-macros [cond-xlet ->hash]]
   [app.ratoms :refer [*num-device-connected *active-port-id]]
   [app.db :as db :refer [*db]]
   [app.serial :as serial :refer [has-web-serial-api?
                                  *ports]]))

(defn add-classes [classes x]
  (str classes " " (if (string? x)
                     x
                     (->> (filter identity x)
                          (interpose " ")
                          (apply str)))))
(defn button [f inner-dom & {:keys [primary error classes]
                             :or {primary false
                                  error false
                                  classes nil}}]
  (into [:button {:class (cond-> "pure-button"
                           primary (str " pure-button-primary")
                           error (str " button-error")
                           classes (add-classes classes))
                  :on-click f}]
        inner-dom))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn nav [args]
  (let [num-devices @*num-device-connected
        xs (->> @*ports
                vals
                (sort-by :i))]
    [:div {:id "nav" :class "pure-u tc"}
     [:h1.f5.mv3 "CharaChorder Config"]

     (into [:div] (map (partial gen-device-buttons args) xs))

     (button serial/request! ["Connect New Device"]
             :primary (= 0 num-devices)
             :classes ["mt2 mb1"
                       (when (< 0 num-devices) "button-xsmall")])
     [:div.f6 num-devices " device(s) connected."]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn no-device-main-view [args]
  [:div {:id "main" :class "pure-u-1 pa3"}
   [:h1 "No Device Connected Yet"]
   [:p "Connect a device using the button on the left."]])

(defn main-view [{:keys [port-id]}]
  [:div {:id "main" :class "pure-u-1 pa3"}
   (button #(serial/disconnect! port-id)
           ["Disconnect"] :error true)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn root-view []
  (let [active-port-id @*active-port-id
        port-id active-port-id
        args (->hash active-port-id port-id)]
    [:div {:id "root" :class "pure-g"}
     [nav args]
     (if-not active-port-id
       [no-device-main-view args]
       [main-view args])]))

(defn super-root-view []
  (cond
    (not (has-web-serial-api?)) [no-web-serial-api-view]
    :else [root-view]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-main-root-element []
  (js/document.getElementById "charachorder-config"))

(defn ^:export render []
  (rdom/render [super-root-view] (get-main-root-element)))

(defn on-dom-content-loaded! []
  (render))

(defn ^:export init []
  (s/check-asserts true)
  (db/init!)
  (serial/init!)
  (js/window.addEventListener "DOMContentLoaded" on-dom-content-loaded!))

