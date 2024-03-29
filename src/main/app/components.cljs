(ns app.components
  (:require ["react-tiny-popover" :as react-tiny-popover]
            [app.macros :as mac :refer-macros [cond-xlet ->hash]]
            [app.utils :refer [get-main-root-element]]
            [clojure.string :as str]
            [datascript.core :refer [squuid]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def Popover react-tiny-popover/Popover)
(assert Popover)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def should-check-for-errors true)

(defn add-classes [classes x]
  (str classes " " (if (string? x)
                     x
                     (->> (filter identity x)
                          (interpose " ")
                          (apply str)))))

(defn concat-classes [& xs]
  (->> (remove str/blank? xs)
       (interpose " ")
       (apply str)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn button [f inner-dom & {:keys [primary secondary error warning minimal active
                                    classes size danger]
                             :or {primary false
                                  secondary false
                                  danger false
                                  error false
                                  warning false
                                  minimal false
                                  classes nil}}]
  (into [:button {:class (cond-> "pure-button"
                           primary (str " pure-button-primary")
                           secondary (str " button-secondary")
                           danger (str " button-error")
                           error (str " button-error")
                           warning (str " button-warning")
                           minimal (str " button-minimal")
                           active (str " button-active")
                           size (str " button-" size)
                           classes (add-classes classes))
                  :on-click f}]
        inner-dom))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn error [& args]
  (apply js/console.warn args))

(defn popover
  "Use this as a function"
  [{:as args :keys [isOpen positions align content type
                    position containerClassName transitionDuration]}
   & els]
  (when should-check-for-errors
    (when position
      (error "Popover: 'position' has no effect"))
    (when containerClassName
      (error "Popover: 'containerClassName' do not use this in architect."))
    (when transitionDuration
      (error "Popover: 'transitionDuration' has no effect"))

    (when (nil? isOpen) (error "Popover: missing property 'isOpen'"))
    (when (nil? positions) (error "Popover: missing property 'positions'"))
    (when (nil? align) (error "Popover: missing property 'align'"))
    (when (nil? content) (error "Popover: missing property 'content'"))
    ; (when (nil? type) (error "Popover: missing architect property 'type'"))
    nil)

  (let [parent (get-main-root-element)
        args (cond-> args
               :always (assoc :parentElement parent))]
    (into [:> Popover args]
          els)))
