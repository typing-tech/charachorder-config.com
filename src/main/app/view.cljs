(ns app.view
  (:require
   [reagent.dom :as rdom]
   [posh.reagent :as posh :refer [pull q]]

   [app.macros :as mac :refer-macros [cond-xlet ->hash]]
   [app.ratoms :refer [*num-device-connected *active-port-id]]
   [app.db :as db :refer [*db]]
   [app.components :refer [button]]
   [app.serial :as serial :refer [has-web-serial-api?
                                  *ports]]
   [app.codes :refer [var-params]]))

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

(defn param-table-row [{:keys [port-id]} param-key raw-value]
  (let [{param-type :type
         values :values
         code :code} (get var-params param-key)

        value (cond
                (nil? raw-value)
                [:span.black "NULL"]
                (= param-type :num-boolean)
                [:span {:class (if raw-value "light-green" "dark-red")}
                 (str raw-value)]

                (= param-key :operating-system)
                (get values raw-value)

                :else
                (str raw-value))]
    [:tr
     [:td.tr (pr-str param-key)]
     [:td value]
     [:td (str param-type)]
     [:td code]]))

(defn param-table [{:as args :keys [port-id]}]
  (let [param-keys (->> (sort-by #(get-in % [1 :code]) var-params)
                        (map first))
        param-values @(pull *db '[*] [:port/id port-id])]
    [:table {:class "pure-table"}
     [:thead
      [:tr [:th "Param"] [:th "Value"] [:th "Type"] [:th "Code"]]]
     (into [:tbody]
           (map (fn [param-key]
                  [param-table-row args param-key (get param-values param-key)])
                param-keys))]))

(defn main-view [{:as args :keys [port-id]}]
  [:div {:id "main" :class "pure-u-1 pa3"}
   [:div {:class "mb2"}
    (button #(serial/refresh-params port-id) ["Refresh Params"])
    (button #(serial/disconnect! port-id) ["Disconnect"] :error true)
    (button #(serial/reset-params! port-id) ["RESET Params and COMMIT"] :error true)]
   [param-table args]])

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
