(ns app.core
  (:require
   [clojure.spec.alpha :as s]
   [cljfmt.core]
   [orchestra-cljs.spec.test :as ost]
   [expound.alpha :as expound]
   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [promesa.core :as p]

   [reagent.dom :as rdom]

   [app.macros :refer-macros [cond-xlet]]
   [app.ratoms :refer [*device-connected]]
   [app.serial :as serial :refer [has-web-serial-api?]]))

(defn button [f inner-dom & {:keys [primary danger]
                             :or {primary true
                                  danger false}}]
  (into [:button {:class (cond-> "pure-button"
                           primary (str " pure-button-primary"))
                  :on-click f}]
        inner-dom))

(defn no-web-serial-api-view []
  [:div {:class "pure-u-1 tc"}
   [:h1.blink "Your browser does not support the Web Serial API."]
   [:p "As of Janurary 2023, the only known browsers to support the Web Serial API is Chrome, Edge, and Opera from April 2021 onwards."]])

(defn main-view []
  [:div
   (button serial/request! ["Connect"])
   (button serial/id ["Test"])
   (button serial/ram ["RAM"])])

(defn root-view []
  (cond
    (not (has-web-serial-api?)) [no-web-serial-api-view]
    :else [main-view]))

(defn get-main-root-element []
  (js/document.getElementById "charachorder-config"))

(defn ^:export render []
  (rdom/render [root-view] (get-main-root-element)))

(defn on-dom-content-loaded! []
  (render))

(defn ^:export init []
  (s/check-asserts true)
  (serial/init!)
  (js/window.addEventListener "DOMContentLoaded" on-dom-content-loaded!))

