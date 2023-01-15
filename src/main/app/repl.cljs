(ns app.repl
  (:require
   [app.serial :refer [*ports]]
   [app.serial.fns :refer [query-all-vars!]]))

(defn f []
  (let [port (-> @*ports first val)]
    (query-all-vars! port)
    nil))

(comment (f))
