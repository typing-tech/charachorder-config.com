(ns app.ratoms
  (:require
   [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce *url-search-params (r/atom {}))
(defonce *nav-expanded (r/atom false))
(defonce *num-devices-connected (r/atom 0))
(defonce *active-port-id (r/atom nil))
(defonce *current-tab-view (r/atom :keymap))
