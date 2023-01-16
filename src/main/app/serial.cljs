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
   [posh.reagent :as posh :refer [transact!]]
   [reagent.core :as r]

   [app.macros :refer-macros [cond-xlet ->hash]]
   [app.ratoms :refer [*num-device-connected *active-port-id]]
   [app.db :refer [*db]]

   [app.serial.fns :refer [issue-connect-cmds!]]))

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

(defn on-serial-connect! [e]
  (cond-xlet
   :do (js/console.log "connect!")
   :do (js/console.log e)
   :return nil))

(defn on-serial-disconnect! [e]
  (cond-xlet
   :do (js/console.log "disconnect!")
   :do (js/console.log e)
   :return nil))

(defn init! []
  (when (has-web-serial-api?)
    (.addEventListener Serial "connect" #(on-serial-connect! %))
    (.addEventListener Serial "disconnect" #(on-serial-disconnect! %))))

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

(defn start-processing-loop! [{:as port :keys [fn-ch]}]
  (assert port)
  (assert fn-ch)
  (go-loop []
    (let [f (<! fn-ch)]
      (when f
        (<! (f port))
        (recur)))))

(defn setup-io-loops! [port-id
                       ^js port]
  (let [read-ch (chan)
        write-ch (chan)
        fn-ch (chan)
        *writer (atom nil)
        *reader (atom nil)
        *device-name (r/atom "???")

        close-port-and-cleanup!
        (fn []
          (when [(get @*ports port-id)]
            (close! fn-ch)
            (close! write-ch)
            (close! read-ch)
            (.close @*writer)

            (-> (.cancel @*reader)
                (p/then #(.close port))
                (p/catch (fn [e]
                           (js/console.log "following exception while closing port")
                           (js/console.warn e))))
            (swap! *ports dissoc port-id)
            (swap! *num-device-connected dec)))

        m (->hash port read-ch write-ch fn-ch *device-name
                  close-port-and-cleanup!)]

    (let [writable (oget port "writable")
          w (.getWriter writable)]
      (reset! *writer w)
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
      (reset! *reader r)
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

    (letfn [(yield-until-ready []
              (if (or (nil? @*writer)
                      (nil? @*reader))
                (js/window.requestIdleCallback yield-until-ready)
                (start-processing-loop! m)))]
      (yield-until-ready))

    m))

(defn add-port-to-list [m {:as x :keys [port-id]}]
  (let [index (count m)
        x (assoc x :i index)
        num-devices (inc index)]
    (reset! *num-device-connected num-devices)
    (reset! *active-port-id port-id)
    (assoc m port-id x)))

(defn register-port! [^js port]
  (cond-xlet
   ;; do not add if port is already added
   (some (fn [x]
           (= port (:port x)))
         (vals @*ports))
   false

   :let [port-id (str (squuid))]
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
   :do (swap! *ports add-port-to-list m)
   :do (transact! *db [{:port/id port-id}])
   :do (issue-connect-cmds! m)
   :return nil))

(defn request! []
  (-> (.requestPort Serial)
      (p/then register-port!)
      (p/catch (fn [e] (js/console.error e)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-port [port-id]
  (-> @*ports
      (get port-id)))

(defn disconnect! [port-id]
  (let [{:keys [close-port-and-cleanup!]} (get-port port-id)]
    (reset! *active-port-id nil)
    (close-port-and-cleanup!)))