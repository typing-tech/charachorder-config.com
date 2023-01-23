(ns app.views.params
  (:require
   [posh.reagent :as posh :refer [pull q]]
   [app.db :as db :refer [*db]]
   [app.codes :refer [var-params]]
   [app.components :refer [button]]
   [app.serial :as serial :refer [has-web-serial-api?]]
   [app.serial.ops :as ops :refer [disconnect!
                                   refresh-params
                                   reset-params!]]
   [app.serial.constants :refer [baud-rates
                                 *ports
                                 dummy-port-id]]))

(defn param-table-row [_ param-key raw-value]
  (let [{param-type :type
         values :values
         code :code} (get var-params param-key)

        value (cond
                (nil? raw-value)
                [:span.mid-gray "NULL"]
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

(defn params-view [{:as args :keys [port-id]}]
  (let []
    [:div {:class "pa3"}
     [:div {:class "mb2"}
      (button #(refresh-params port-id) ["Refresh Params"])
      (button #(disconnect! port-id) ["Disconnect"] :error true)
      (button #(reset-params! port-id) ["RESET Params and COMMIT"] :error true)]
     [param-table args]]))

