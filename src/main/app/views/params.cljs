(ns app.views.params
  (:require
   [posh.reagent :as posh :refer [pull q]]
   [app.db :as db :refer [*db]]
   [app.codes :refer [var-params]]
   [app.components :refer [button]]
   [app.serial.ops :as ops :refer [disconnect!
                                   refresh-params
                                   reset-params!]]
   [app.serial.constants :refer [baud-rates
                                 *ports
                                 dummy-port-id]]))

(defn boolean-control [port-id param-key curr]
  (let []
    [:label {:class "switch"}
     [:input {:type "checkbox"
              :checked curr
              :on-change #(ops/set-param! port-id param-key (not curr))}]
     [:span {:class "slider round"}]]))

(defn param-table-row [{:keys [port-id]} param-key raw-value]
  (let [{param-type :type
         values :values
         code :code
         label :label} (get var-params param-key)

        value (cond
                (nil? raw-value)
                [:span.mid-gray "NULL"]

                (= param-type :num-boolean)
                [boolean-control port-id param-key raw-value]

                (= param-key :operating-system)
                (get values raw-value)

                :else
                (str raw-value))]
    [:tr
     [:td.tr (if label
               (into [:<>] label)
               (pr-str param-key))]
     [:td value]
     [:td (str param-type)]
     [:td code]]))

(defn param-tables [{:as args :keys [port-id]}]
  (let [param-keys (->> (sort-by #(get-in % [1 :code]) var-params)
                        (map first))
        ;; partition a sequence in half
        [param-keys1 param-keys2] (partition-all (int (/ (count param-keys) 2)) param-keys)
        param-values @(pull *db '[*] [:port/id port-id])]
    (into
     [:<>]
     (for [ks [param-keys1 param-keys2]]
       [:div {:class "dib v-top mr4"}
        [:table {:class "pure-table"}
         [:thead
          [:tr [:th "Param"] [:th "Value"] [:th "Type"] [:th "Code"]]]
         (into [:tbody]
               (map (fn [param-key]
                      [param-table-row args param-key (get param-values param-key)])
                    ks))]]))))

(defn params-view [{:as args :keys [port-id]}]
  (let []
    [:div {:class "pa3"}
     [:div {:class "mb2"}
      (button #(refresh-params port-id) ["Refresh Params"])]

     [param-tables args]]))

