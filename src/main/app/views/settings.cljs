(ns app.views.settings
  (:require
   [reagent.core :as r]
   [app.components :refer [button]]
   [app.settings :as settings]))

(def *force-rerender (r/atom 0))

(defn settings-view []
  (let [x @*force-rerender]
    [:div {:class "pa3 mb5" :data-dummy x}
     (let [cc1-compact-mode (settings/get :cc1-compact-mode false)]
       (button #(do (settings/set! :cc1-compact-mode (not cc1-compact-mode))
                    (swap! *force-rerender inc))
               (if cc1-compact-mode
                 ["Turn off CC1 Compact Layout"]
                 ["Switch to CC1 Compact Layout"])
               :active cc1-compact-mode))]))
