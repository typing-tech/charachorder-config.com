(ns app.serial.constants)

(def baud-rates {;; CC1 M0
                 [9114 32783] 115200
                 ;; CC Lite M0
                 [9114 32796] 115200})
(defonce *ports (atom {}))
(def dummy-port-id "0")

(defn get-port [port-id]
  (-> @*ports
      (get port-id)))
