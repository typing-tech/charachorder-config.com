(ns app.ratoms
  (:require [app.settings :as settings]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce *url-search-params (r/atom {}))
(defonce *nav-expanded (r/atom false))
(defonce *num-devices-connected (r/atom 0))
(defonce *active-port-id (r/atom nil))
(defonce *current-tab-view (r/atom (settings/get :last-view :keymap)))
