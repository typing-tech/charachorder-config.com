(ns app.views.settings
  (:require
   [reagent.core :as r]
   [app.components :refer [button]]
   [app.settings :as settings]))

(def *force-rerender (r/atom 0))

(defn settings-view []
  (let [x @*force-rerender]
    [:div {:class "pa3 mb5" :data-dummy x}
     ;; CC1 Compact Mode
     (let [cc1-compact-mode (settings/get :cc1-compact-mode false)]
       (button #(do (settings/set! :cc1-compact-mode (not cc1-compact-mode))
                    (swap! *force-rerender inc))
               (if cc1-compact-mode
                 ["Turn off CC1 Compact Layout"]
                 ["Switch to CC1 Compact Layout"])
               :active cc1-compact-mode))
     ;; Advanced Parameters
      (let [advanced-params (settings/get :advanced-params false)]
        (button #(do (settings/set! :advanced-params (not advanced-params))
                      (swap! *force-rerender inc))
                (if advanced-params
                  ["Turn off Advanced Parameters"]
                  ["Turn on Advanced Parameters"])
                :active advanced-params))
     ]))
