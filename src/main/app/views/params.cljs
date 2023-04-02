(ns app.views.params
  (:require [app.codes :refer [var-params]]
            [app.codes :refer [var-params]]
            [app.components :refer [button concat-classes popover]]
            [app.db :as db :refer [*db]]
            [app.macros :as mac]
            [app.serial.ops :as ops :refer [disconnect! refresh-params
                                            reset-params!]]
            [app.settings :as settings]
            [posh.reagent :as posh :refer [pull q]]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def user-allowed-mod-time-in-secs 2000)
(def number-debounce-time-in-ms 1000)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn boolean-control [port-id param-key curr]
  (let []
    [:label {:class "switch"}
     [:input {:type "checkbox"
              :checked curr
              :on-change #(ops/set-param! port-id param-key (not curr))}]
     [:span {:class "slider round"}]]))

(defn number-control-popup [port-id param-key {:keys [min max step defaults]}
                            *curr *focused]
  (r/as-element
   [:div {:on-mouse-down #(do (.stopPropagation %)
                              (.preventDefault %))
          :on-mouse-up #(.stopPropagation %)
          :on-click #(.stopPropagation %)}
    "min: " min [:div.dib.mh2]
    "max: " max [:div.dib.mh2]
    "step:" step
    [:div.mb2]
    (when defaults
      (into
       [:div.tc [:div.tc.mb1 "Defaults:"]]
       (for [[label val] defaults]
         [:div
          (button (fn []
                    (ops/set-param! port-id param-key val)
                    (reset! *curr val)
                    (reset! *focused false))
                  label
                  :size "small" :classes ["mr0" "mb1"])])))]))

(defn number-control []
  (let [*curr (r/atom nil)
        *focused (r/atom false)
        *tid (atom nil)
        *change-tid (atom nil)
        on-change!
        (fn [port-id param-key e]
          (let [val (-> e .-target .-value js/parseInt)]
            (reset! *curr val)
            ;; TODO: debounce
            (when-let [tid @*change-tid] (js/clearTimeout tid))
            (reset! *change-tid (js/setTimeout
                                 (fn []
                                   (js/console.log "SEND" (pr-str param-key) val)
                                   (ops/set-param! port-id param-key val))
                                 number-debounce-time-in-ms))))]
    (fn [port-id param-key curr]
      (cond
        (nil? @*curr) (reset! *curr curr)
        (not= @*curr curr)
        (do (when-let [tid @*tid] (js/clearTimeout tid))
            (reset! *tid (js/setTimeout
                          (fn []
                            (js/console.log "BASH" (pr-str param-key) curr)
                            (reset! *curr curr))
                          user-allowed-mod-time-in-secs))))
      (let [{:as param :keys [min max step]} (get var-params param-key)]
        ;; (js/console.log param)
        (popover
         {:isOpen @*focused
          :positions ["bottom"]
          :align "end"
          :reposition true
          :content (number-control-popup port-id param-key param
                                         *curr *focused)}
         [:div
          [:input (cond-> {:type "number"
                           :value @*curr
                           :on-focus #(reset! *focused true)
                           :on-blur #(reset! *focused false)
                           :on-change #(on-change! port-id param-key %)}
                    step (assoc :step step)
                    min (assoc :min min)
                    max (assoc :max max))]])))))

(defn dropdown-control [port-id param-key curr]
  (let [{:as param :keys [values]} (get var-params param-key)]
    (into [:select {:value curr
                    :on-change #(ops/set-param! port-id param-key (-> % .-target .-value))}]
          (for [[k v] values]
            [:option {:value k
                      :selected (= k curr)}
             v]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def number-types #{:ms :us :mouse :pos-int :non-neg-int})

(defn param-table-row [{:keys [port-id is-advanced]}
                       param-key raw-value]
  (let [{param-type :type
         values :values
         code :code
         label :label} (get var-params param-key)

        value (cond
                (nil? raw-value)
                [:span.mid-gray "N/A"]

                (= param-type :num-boolean)
                [boolean-control port-id param-key raw-value]

                (= param-type :dropdown)
                [dropdown-control port-id param-key raw-value]

                (contains? number-types param-type)
                [number-control port-id param-key raw-value]

                (= param-key :operating-system)
                (get values raw-value)

                :else
                (str raw-value))]
    [:tr
     [:td.tr (if label
               (into [:<>] label)
               (pr-str param-key))]
     [:td {:class (concat-classes "tc"
                                  (when (nil? raw-value) "bg-black"))} value]
     (when is-advanced [:td (str param-type)])
     (when is-advanced [:td code])]))

(defn param-tables [{:as args :keys [port-id]}]
  (let [is-advanced (settings/get :advanced-params false)
        param-keys (->> (sort-by #(get-in % [1 :code]) var-params)
                        (remove #(and (not is-advanced)
                                      (get-in % [1 :advanced])))
                        (map first))
        ;; partition a sequence in half
        [param-keys1 param-keys2] (partition-all (js/Math.round (/ (count param-keys) 2.0)) param-keys)
        param-values @(pull *db '[*] [:port/id port-id])
        
        args (mac/args is-advanced)]
    (into
     [:<>]
     (for [ks [param-keys1 param-keys2]]
       [:div {:class "dib v-top mr4"}
        [:table {:class "pure-table"}
         [:thead
          [:tr [:th "Param"] [:th "Value"]
           (when is-advanced [:th "Type"])
           (when is-advanced [:th "Code"])]]
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

