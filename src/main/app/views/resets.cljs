(ns app.views.resets
  (:require
   [posh.reagent :as posh :refer [pull q]]
   [app.db :as db :refer [*db]]
   [app.codes :refer [var-params]]
   [app.components :refer [button]]
   [app.serial.ops :as ops]))

(defn resets-view [{:keys [port-id]}]
  (let []
    [:div {:class "pa3"}
     (button #(ops/reset-keymaps! port-id) ["RESET keymaps and COMMIT"] :error true)
     (button #(ops/factory-reset! port-id) ["FACTORY RESET"] :error true)]))
