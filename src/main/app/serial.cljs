(ns app.serial
  (:require
   [cljs.core.async :as async
    :refer [chan <! >! onto-chan! close!]
    :refer-macros [go go-loop]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [clojure.string :as str]

   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [promesa.core :as p]
   [datascript.core :refer [squuid]]

   [app.macros :refer-macros [cond-xlet ->hash]]
   [app.ratoms :refer [*device-connected]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^js Serial (oget js/navigator "?serial"))
(when-not Serial (js/console.error "This browser does not have WebSerial API."))

(defn has-web-serial-api? []
  (boolean Serial))

(def cmd-encoder (new js/TextEncoder))
(def output-decoder (new js/TextDecoder))

(def baud-rates {[9114 32783] 115200})
(defonce *ports (atom {}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn connect! [e]
  (cond-xlet
   :do (js/console.log "connect!")
   :do (js/console.log e)
   :return nil))

(defn disconnect! [e]
  (cond-xlet
   :do (js/console.log "disconnect!")
   :do (js/console.log e)
   :return nil))

(defn init! []
  (when (has-web-serial-api?)
    (.addEventListener Serial "connect" #(connect! %))
    (.addEventListener Serial "disconnect" #(disconnect! %))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn concat-uint8-array [a b]
  (let [a-length (oget a "length")
        c (new js/Uint8Array (+ a-length (oget b "length")))]
    (.set c a)
    (.set c b a-length)
    c))

(defn parse-lines [^js buf]
  ;; 10 is "\n"
  (let [i (.lastIndexOf buf 10)]
    (if (< i 0)
      [[] buf]
      (let [bites (.subarray buf 0 (inc i))
            s (.decode output-decoder bites)
            lines (str/split s "\r\n")
            rem (.subarray buf (inc i))]
        [lines rem]))))

(defn setup-io-loops! [port-id
                       ^js port]
  (let [read-ch (chan)
        write-ch (chan)

        close-port-and-cleanup!
        (fn []
          (close! write-ch)
          (close! read-ch)
          (.close port)
          (swap! *ports dissoc port-id)
          (reset! *device-connected false))]
    (let [writable (oget port "writable")
          w (.getWriter writable)]
      (go-loop []
        (try
          (if-some [x (<! write-ch)]
            (do (->> (str x "\r\n")
                     (.encode cmd-encoder)
                     (.write w)
                     <p!)
                (recur))
            (.releaseLock w))
          (catch :default e
            (close-port-and-cleanup!)
            (js/console.error e)))))

    (let [readable (oget port "readable")
          r (.getReader readable)]
      (go-loop [prev-buf (new js/Uint8Array)]
        (try
          (let [x (<p! (.read r))
                value (oget x "value")
                done (oget x "done")]
            (if done
              (.releaseLock r)
              (let [buf (concat-uint8-array prev-buf value)
                    [lines rem] (parse-lines buf)]
              ; (js/console.log [lines rem])
                (<! (onto-chan! read-ch lines false))
                (recur rem))))
          (catch :default e
            (close-port-and-cleanup!)
            (js/console.error e)))))

    (->hash read-ch write-ch)))

(defn register-port! [^js port]
  (cond-xlet
   :let [port-id (squuid)]
   :plet [info (.getInfo port)]

   :let [usbVendorId (oget info "usbVendorId")
         usbProductId (oget info "usbProductId")
         hardware-id [usbVendorId usbProductId]]
   :do (js/console.log "usbVendorId" usbVendorId "usbProductId" usbProductId)

   :let [baud-rate (get baud-rates hardware-id)]
   :do (if-not baud-rate
         (js/console.error "unable to determine appropriate baud rate for device")
         (js/console.log "using baud rate" baud-rate))

   ;; Promise does not return anything
   :pdo (.open port #js {:baudRate baud-rate})

   :do (js/console.log "opened")
   :let [m (merge {:port-id port-id
                   :hardware-id hardware-id}
                  (setup-io-loops! port-id port))]
   ; :do (js/console.log m)
   :do (swap! *ports assoc port-id m)
   :return nil))

(defn request! []
  (-> (.requestPort Serial)
      (p/then register-port!)
      (p/catch (fn [e] (js/console.error e)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-any-port []
  (-> @*ports first val))

(defn id []
  (let [m (get-any-port)
        {:keys [read-ch write-ch]} m]
    (go
      (>! write-ch "ID")
      (-> (<! read-ch) (js/console.log)))))

(defn ram []
  (let [m (get-any-port)
        {:keys [read-ch write-ch]} m]
    (go
      (>! write-ch "RAM")
      (-> (<! read-ch) (js/console.log)))))
