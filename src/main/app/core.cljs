(ns app.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [orchestra-cljs.spec.test :as ost]
   [expound.alpha :as expound]
   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [promesa.core :as p]

   [reagent.dom :as rdom]
   [posh.reagent :as posh :refer [pull q transact!]]

   [app.macros :as mac :refer-macros [cond-xlet ->hash]]
   [app.utils :refer [get-main-root-element]]
   [app.ratoms :refer [*url-search-params
                       *num-device-connected
                       *active-port-id]]
   [app.db :as db :refer [*db]]
   [app.serial :as serial :refer [has-web-serial-api? *ports]]
   [app.view :refer [super-root-view]]
   [app.csv :refer [load-csv-text!]]

   [app.repl]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^:export render []
  (rdom/render [super-root-view] (get-main-root-element)))

(defn on-dom-content-loaded! []
  (render))

(defn update-url-search-params! []
  (let [params (oget js/window "location.search")
        obj (new js/URLSearchParams params)]
    (reset! *url-search-params obj)))

(defn update-layout-from-url! []
  (when (.has @*url-search-params "cc1-layout")
    (transact! *db [{:port/id 0}])
    (swap! *num-device-connected inc)
    (reset! *active-port-id 0)
    (let [csv (.get @*url-search-params "cc1-layout")]
      (when-not (str/blank? csv)
        (load-csv-text! 0 csv)))))

(defn on-url-change! [_e]
  (js/console.log "updating layout due to URL change!")
  (update-url-search-params!)
  (update-layout-from-url!))

(defn ^:export init []
  (s/check-asserts true)

  (update-url-search-params!)
  (update-layout-from-url!)

  (db/init!)
  (serial/init!)

  (.addEventListener js/window "popstate" #(on-url-change! %))

  (js/window.addEventListener "DOMContentLoaded" on-dom-content-loaded!))
