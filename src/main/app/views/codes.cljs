(ns app.views.codes 
  (:require [app.codes :refer [keymap-codes]]))

(defn- gen-row [{:keys [code type action action-desc notes]}]
  [:tr
   [:td code]
   [:td type]
   [:td action]
   [:td action-desc]
   [:td.measure-wide notes]])

(defn codes-view []
  (let []
    [:div {:class "pa3 pa0"}
     [:h1 "Action Codes"]
     [:table {:class "mt3 pure-table pure-table-bordered"}
      [:thead
       [:tr
        [:th "Code"]
        [:th "Type"]
        [:th "Action"]
        [:th "Description"]
        [:th "Notes"]]]
      (into [:tbody]
            (map gen-row keymap-codes))]]))
