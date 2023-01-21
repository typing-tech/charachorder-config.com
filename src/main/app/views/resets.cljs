(ns app.views.resets
  (:require
   [posh.reagent :as posh :refer [pull q]]
   [app.db :as db :refer [*db]]
   [app.codes :refer [var-params]]
   [app.components :refer [button]]
   [app.serial :as serial]))

(defn resets-view [{:keys [port-id]}]
  (let []
    [:div {:class "pa3"}
     (button #(serial/reset-keymaps! port-id) ["RESET keymaps and COMMIT"] :error true)
     (button #(serial/factory-reset! port-id) ["FACTORY RESET"] :error true)]))
