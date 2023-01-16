(ns app.components
  (:require
   [app.macros :as mac :refer-macros [cond-xlet ->hash]]
   [app.ratoms :refer [*num-device-connected *active-port-id]]))

(defn add-classes [classes x]
  (str classes " " (if (string? x)
                     x
                     (->> (filter identity x)
                          (interpose " ")
                          (apply str)))))
(defn button [f inner-dom & {:keys [primary error classes]
                             :or {primary false
                                  error false
                                  classes nil}}]
  (into [:button {:class (cond-> "pure-button"
                           primary (str " pure-button-primary")
                           error (str " button-error")
                           classes (add-classes classes))
                  :on-click f}]
        inner-dom))
