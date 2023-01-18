(ns app.ratoms
  (:require
   [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce *nav-expanded (r/atom false))
(defonce *num-device-connected (r/atom 0))
(defonce *active-port-id (r/atom nil))
(defonce *current-tab-view (r/atom :keymap))
