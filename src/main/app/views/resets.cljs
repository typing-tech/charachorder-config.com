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
     [:div.mb5
      (button #(ops/reset-restart! port-id) ["Restart Device"])
      (button #(ops/reset-bootloader! port-id) ["Enter Bootloader Mode"])]
     [:div.mb5
      (button #(ops/reset-starter! port-id) ["Add Starter Chords"] :warning true)
      (button #(ops/reset-func! port-id) ["Add Functional Chords (BS, DEL, Arrow)"] :warning true)]
     [:div.mb5
      (button #(ops/reset-keymaps! port-id) ["RESET keymaps and COMMIT"] :danger true)
      (button #(ops/reset-clearcml! port-id) ["DELETE ALL Chords"] :danger true)
      (button #(ops/factory-reset! port-id) ["FACTORY RESET"] :danger true)]]))
