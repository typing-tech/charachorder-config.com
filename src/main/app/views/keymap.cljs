(ns app.views.keymap
  (:require
   [reagent.core :as r]
   [app.components :refer [button popover]]
   [app.db :as db :refer [*db]]))

(defn code-table [port-id]
  [:div {:class ""}
   [:div {} "Tabs"]
   [:div {} "Codes"]])

(defn code-chooser-com []
  (let []
    (fn [port-id]
      (let []
        (popover
         {:isOpen true
          :positions ["top" "bottom" "right" "left"]
          :align "start"
          :reposition true
          :content (r/as-element [code-table port-id])}
         [:div {:class "pointer"} "click"])))))

(defn keymap-view [{:keys [port-id]}]
  (let []
    [code-chooser-com port-id]))
