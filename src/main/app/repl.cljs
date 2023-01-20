(ns app.repl
  (:require
   [app.serial :refer [*ports]]
   [app.serial.fns :refer [query-all-var-params!
                           query-all-var-keymaps!]]))

(defn f []
  (let [port (-> @*ports first val)]
    ; (query-all-var-params! port)
    (query-all-var-keymaps! port)
    nil))

(comment (f))
