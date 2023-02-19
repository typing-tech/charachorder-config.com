(ns app.serial
  (:require
   [cljs.core.async :as async
    :refer [chan <! >! onto-chan! close! put!]
    :refer-macros [go go-loop]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [clojure.string :as str]

   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   [promesa.core :as p]
   [datascript.core :refer [squuid]]
   [posh.reagent :as posh :refer [transact!]]
   [datascript.core :as ds]
   [reagent.core :as r]

   [app.macros :refer-macros [cond-xlet ->hash]]
   [app.ratoms :refer [*num-devices-connected *active-port-id]]
   [app.db :refer [*db]]
   [app.utils :refer [timestamp-ms
                      human-time-with-seconds]]

   [app.emoji-strings :refer [keyboard-left-arrow keyboard-right-arrow]]
   [app.hw.cc1 :as cc1]
   [app.csv :as csv]
   [app.serial.constants :refer [baud-rates
                                 *ports
                                 get-port
                                 dummy-port-id]]
   [app.serial.fns :as fns :refer [issue-connect-cmds!]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^js Serial (oget js/navigator "?serial"))
(when-not Serial (js/console.error "This browser does not have WebSerial API."))

(defn has-web-serial-api? [] (boolean Serial))
(def is-valid-response-output?
  (partial re-matches #"(CMD|ID|VERSION|CML|VAR|RST|RAM|SIM)\s.+"))

(def cmd-encoder (new js/TextEncoder))
(def output-decoder (new js/TextDecoder))

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
        *device-version (r/atom "???")
        *api-log (r/atom {})
        *api-log-size (r/atom 0)
        *serial-log (r/atom {})
        *serial-log-size (r/atom 0)
        *console (r/atom [])
        *ready (r/atom false)

        write-to-api-log!
        (fn [stdin]
          (let [i @*api-log-size
                stdin-t (timestamp-ms)
                x (->hash stdin stdin-t)]
            (swap! *api-log update i merge x)))
        read-to-api-log!
        (fn [stdout]
          (let [i @*api-log-size
                stdout-t (timestamp-ms)
                x (->hash stdout stdout-t)]
            (swap! *api-log update i merge x)
            (swap! *api-log-size inc)))
        read-to-serial-log!
        (fn [stdout]
          (let [i @*serial-log-size
                stdout-t (timestamp-ms)
                x (->hash stdout stdout-t)]
            (swap! *serial-log update i merge x)
            (swap! *serial-log-size inc)))

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
            (reset! *num-devices-connected (-> @*ports count))))

        m (->hash port-id port read-ch write-ch fn-ch *device-name *device-version *ready
                  *api-log *serial-log read-to-serial-log!
                  close-port-and-cleanup!)]

    (let [writable (oget port "writable")
          w (.getWriter writable)]
      (reset! *writer w)
      (go-loop []
        (try
          (if-some [x (<! write-ch)]
            (do (write-to-api-log! x)
                (->> (str x "\r\n")
                     (.encode cmd-encoder)
                     (.write w)
                     <p!)
                (recur))
            (.releaseLock w))
          (catch :default e
            (close-port-and-cleanup!)
            (js/console.error e)))))

    (let [readable (oget port "readable")
          r (.getReader readable)
          flush-lines!
          (fn [lines*]
            (go-loop [lines lines*]
              (when-let [x (first lines)]
                (if (is-valid-response-output? x)
                  (do (read-to-api-log! x)
                      (>! read-ch x))
                  (do (read-to-serial-log! x)
                      nil))
                (recur (rest lines)))))]
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
                (<! (flush-lines! lines))
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
    (reset! *num-devices-connected num-devices)
    (reset! *active-port-id port-id)
    (assoc m port-id x)))

(defn on-device-connect! [{:as port :keys [port-id]}]
  (go
    (<! (issue-connect-cmds! port))
    (csv/update-url-from-db! port-id)))

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
   :let [port (get @*ports port-id)]
   :do (transact! *db [{:port/id port-id}])
   :return (on-device-connect! port)))

(defn request! []
  (-> (.requestPort Serial)
      (p/then register-port!)
      (p/catch (fn [e] (js/console.error e)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

