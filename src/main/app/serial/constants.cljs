(ns app.serial.constants)

(def baud-rates {[9114 32783] 115200})
(defonce *ports (atom {}))
(def dummy-port-id "0")
