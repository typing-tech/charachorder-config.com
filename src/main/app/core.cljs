(ns app.core
  (:require
   [devtools.core]))

(defn ^:export init []
  (js/console.log "INIT"))

(defn ^:export render []
  (js/console.log {:foo :bar})
  (js/console.log "RENDER"))
