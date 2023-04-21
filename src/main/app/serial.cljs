(ns app.serial
  (:require [app.csv :as csv]
            [app.db :refer [*db]]
            [app.macros :refer-macros [cond-xlet ->hash]]
            [app.ratoms :refer [*active-port-id *num-devices-connected]]
            [app.serial.constants :refer [*ports baud-rates dummy-port-id
                                          get-port]]
            [app.serial.fns :refer [query-all-var-keymaps!
                                    query-all-var-params! store-device-name store-device-version]]
            [app.serial.ops :refer [query-all-chordmaps!]]
            [app.utils :refer [human-time-with-seconds
                               parse-binary-chord-string timestamp-ms]]
            [cljs.core.async :as async
             :refer [<! >! chan close! onto-chan! put!]
             :refer-macros [go go-loop]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.string :as str]
            [datascript.core :as ds :refer [squuid]]
            [oops.core :refer [oapply oapply! oapply!+ oapply+ ocall ocall!
                               ocall!+ ocall+ oget oget+ oset! oset!+]]
            [posh.reagent :as posh :refer [transact!]]
            [promesa.core :as p]
            [reagent.core :as r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^js Serial (oget js/navigator "?serial"))
(when-not Serial (js/console.error "This browser does not have WebSerial API."))

(defn has-web-serial-api? [] (boolean Serial))
(def is-valid-response-output?
  (partial re-matches #"(\d+\s+)?(CMD|ID|-betaID|VERSION|CML|VAR|RST|RAM|SIM)\s.+"))

(def chordmap-read-msg? (partial re-matches #"^Chordmaps_::read_uint128.+"))
(def binary-chord-string? (partial re-matches #"^\d{128}$"))
(def strange-chord? (partial re-matches #"^Strange chord: (\d{128}.*)$"))
(def unmodified-chord? (partial re-matches #"^unmodified chord: (\d{128}.*)$"))
(def nil-chords #{"00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"})

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

(defn find-max-key [m]
  (->> m keys (reduce max js/Number.MIN_SAFE_INTEGER)))
(defn find-min-key [m]
  (->> m keys (reduce min js/Number.MAX_SAFE_INTEGER)))

(def log-limit 200)
(defn add-entry-to-atom! [*log stdout]
  (let [i (-> (find-max-key @*log)
              inc)
        stdout-t (timestamp-ms)
        x (->hash stdout stdout-t)
        f (fn [m]
            (if (>= (count m) log-limit)
              (-> m
                  (dissoc (find-min-key m))
                  (assoc i x))
              (assoc m i x)))]
    (swap! *log f)))

(defn log-bcs [bcs chunks]
  (js/console.debug "BCS" bcs)
  (js/console.debug "CHORD" chunks)
  nil)

(defn set-bcs-if-valid! [bcs *binary-chord-string]
  (let [{:keys [chunks]} (parse-binary-chord-string bcs)]
    (log-bcs bcs chunks)
    (reset! *binary-chord-string bcs)))

(defn detect-chord! [x *should-consume-unprefixed-chord-string *binary-chord-string]
  (cond-xlet
   :let [match (unmodified-chord? x)] 
   match (do (let [[_ bcs] match]
               (set-bcs-if-valid! bcs *binary-chord-string)))

   (chordmap-read-msg? x)
   (reset! *should-consume-unprefixed-chord-string true)

   (and @*should-consume-unprefixed-chord-string
        (contains? nil-chords x))
   (reset! *should-consume-unprefixed-chord-string false)

   (and @*should-consume-unprefixed-chord-string
        (binary-chord-string? x))
   (do (reset! *should-consume-unprefixed-chord-string false)
       (set-bcs-if-valid! x *binary-chord-string)
       nil)

   :let [match (strange-chord? x)]
   (not match) nil
   :let [[_ bcs] match]
   (contains? nil-chords bcs) nil
   :return (set-bcs-if-valid! bcs *binary-chord-string)))

(defn setup-io-loops! [port-id
                       ^js port]
  (let [read-ch (chan)
        write-ch (chan)
        fn-ch (chan)
        *writer (atom nil)
        *reader (atom nil)
        *device-name (r/atom "???")
        *device-version (r/atom "???")

        *chords (atom [])
        *num-chords (r/atom 0)
        *is-reading-chords (r/atom false)
        *chord-read-index (r/atom 0)
        *chord-sorting-method (r/atom :chord)

        *api-log (r/atom {})
        *api-log-size (r/atom 0)
        *serial-log (r/atom {})
        *should-consume-unprefixed-chord-string (atom false)
        *binary-chord-string (r/atom "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
        *ready (r/atom false)

        write-to-api-log!
        (fn [stdin]
          (let [i @*api-log-size
                stdin-t (timestamp-ms)
                x (->hash stdin stdin-t)]
            (js/console.log (str "%c" stdin) "color: #008800")
            (swap! *api-log update i merge x)))
        read-to-api-log!
        (fn [stdout]
          (let [i @*api-log-size
                stdout-t (timestamp-ms)
                x (->hash stdout stdout-t)]
            (js/console.log (str "%c" stdout) "color: #0000ff")
            (swap! *api-log update i merge x)
            (swap! *api-log-size inc)))
        read-to-serial-log!
        (fn [stdout]
          (js/console.debug stdout)
          (add-entry-to-atom! *serial-log stdout))

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
                  *api-log *serial-log read-to-serial-log! *binary-chord-string
                  *chords *num-chords *is-reading-chords *chord-read-index *chord-sorting-method
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
                      (let [[_ serial-header-prefix] (re-find #"^(\d+\s+).+" x)
                            x (if serial-header-prefix
                                (subs x (count serial-header-prefix))
                                x)]
                        ;; (js/console.log "putting on read-ch" x)
                        (>! read-ch x)))
                  (do (detect-chord! x *should-consume-unprefixed-chord-string *binary-chord-string)
                      (read-to-serial-log! x)))
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

(defn issue-connect-cmds! [{:as port :keys [port-id fn-ch *ready]}]
  (go
    (>! fn-ch store-device-name)
    (>! fn-ch store-device-version)
    (<! (query-all-var-params! port))
    (<! (query-all-var-keymaps! port :boot true))
    ;; (<! (query-all-chordmaps! port))
    (reset! *ready true)))

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

