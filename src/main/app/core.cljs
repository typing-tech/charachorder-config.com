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
                       *num-devices-connected
                       *active-port-id]]
   [app.db :as db :refer [*db]]
   [app.serial.constants :refer [*ports
                                 dummy-port-id]]
   [app.serial :as serial :refer [has-web-serial-api?]]
   [app.view :refer [super-root-view]]
   [app.csv :refer [load-compressed-csv-text!]]

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
  (dorun
   (for [param ["cc-lite-layout" "cc1-layout"]]
     (when (.has @*url-search-params param)
       (transact! *db [{:port/id dummy-port-id}])
       (swap! *num-devices-connected inc)
       (reset! *active-port-id dummy-port-id)
       (let [csv (.get @*url-search-params param)]
         (when-not (str/blank? csv)
           (load-compressed-csv-text! dummy-port-id csv)))))))

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

  (js/window.addEventListener "DOMContentLoaded" on-dom-content-loaded!)
  (js/console.log "init done"))
