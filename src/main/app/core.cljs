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
   [app.serial :as serial :refer [has-web-serial-api? *ports]]
   [app.view :refer [super-root-view]]
   
   [app.repl]))

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

