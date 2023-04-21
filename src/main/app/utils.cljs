(ns app.utils
  (:require ["date-fns" :as date-fns]
            [app.macros :refer-macros [cond-xlet ->hash]]
            [cljs.cache :as cache]
            [clojure.set :as set]
            [clojure.string :as str]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-main-root-element []
  (js/document.getElementById "charachorder-config"))

(defn debug-pipe
  ([x]
   (js/console.log x)
   x)
  ([x msg]
   (js/console.log msg x)
   x))

(defn bimap [m]
  (merge m (set/map-invert m)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn timestamp-ms []
  (.now js/Date))

(def *human-time-cache (atom (cache/lru-cache-factory {} :threshold 64)))

(defn human-time-with-seconds* [t] (date-fns/formatISO9075 t))
(defn human-time-with-seconds
  ([] (human-time-with-seconds (timestamp-ms)))
  ([t]
   (swap! *human-time-cache
          #(cache/through human-time-with-seconds* % t))
   (get @*human-time-cache t)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn slow-pad-left [s n]
  (let [s (str s)]
    (if (< (count s) n)
      (str (apply str (repeat (- n (count s)) "0")) s)
      s)))

(defn pad-left [s n]
  (.padStart s n "0"))

(defn lex-comp-numbers [xs]
  (->> (map (fn [x] (-> (str x) (pad-left 3))) xs)
       (apply str)))

(defn binary->decimal
  "Unprefixed binary string."
  [b]
  (-> (str "0b" b)
      (js/BigInt)))

(defn binary->hex
  "Unprefixed binary string."
  [b]
  (-> (str "0b" b)
      (js/BigInt)
      (.toString 16)
      (str/upper-case)
      (pad-left 32)))

(defn hex-str->bin-str
  "Unprefixed hex string."
  [hex-str]
  (-> (str "0x" hex-str)
      (js/BigInt)
      (.toString 2)
      (pad-left 128)))

(defn small-hex->decimal
  "two digit hex string to decimal"
  [hex-str]
  (-> (str "0x" hex-str)
      (js/BigInt)
      (js/Number)))

(def small-binary->decimal
  (comp (map binary->decimal)
        (map js/Number)))

(def parse-binary-chord-string-cache
  (atom (cache/lru-cache-factory {} :threshold 16384)))

(defn parse-binary-chord-string*
  "`chunks` is a vector of 12 action codes, each 10 bits long, represented as an integer."
  [full-bcs]
  (cond-xlet
   :do (assert (string? full-bcs))
   :do (assert (= 128 (count full-bcs)))
   :let [unused-binary-string (subs full-bcs 0 8)
         bcs (subs full-bcs 8)]
   :do (assert (= 120 (count bcs)))
   :let [bs (vec (for [i (range 12)]
                   (subs bcs (* 10 i) (* 10 (+ i 1)))))
         chunks (into [] small-binary->decimal bs)]
   :return (->hash unused-binary-string chunks)))

(defn parse-binary-chord-string [full-bcs]
  (swap! parse-binary-chord-string-cache
         #(cache/through parse-binary-chord-string* % full-bcs))
  (get @parse-binary-chord-string-cache full-bcs))

(defn hex-chord-string->sorted-chunks* [hex-chord-string]
  (let [bcs (if (= 128 (count hex-chord-string))
              hex-chord-string
              (hex-str->bin-str hex-chord-string))]
    (->> bcs
         parse-binary-chord-string
         :chunks
         (filter #(not= 0 %))
         sort
         vec)))

(def hex-chord-string->sorted-chunks-cache (atom (cache/lru-cache-factory {} :threshold 16384)))
(defn hex-chord-string->sorted-chunks [hex-chord-string]
  (swap! hex-chord-string->sorted-chunks-cache
         #(cache/through hex-chord-string->sorted-chunks* % hex-chord-string))
  (get @hex-chord-string->sorted-chunks-cache hex-chord-string))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def variable-length-prefixes #{"01" "02" "03" "04"})

(defn phrase->chunks [phrase]
  (let [xs (for [i (range (/ (count phrase)
                             2))]
             (subs phrase (* 2 i) (* 2 (+ i 1))))
        xs (vec xs)
        n (count xs)
        chunks
        (loop [i 0
               chunks []]
          (cond-xlet
           (<= n i) chunks
           :let [x (nth xs i)
                 is-wide (contains? variable-length-prefixes x)
                 x (if is-wide
                     (str x (nth xs (inc i)))
                     x)] 
           :else (recur (if is-wide
                          (+ i 2)
                          (inc i))
                        (conj chunks x))))]
    (mapv small-hex->decimal chunks)))
;; (js/console.log (phrase->chunks "01386061"))

(defn chunks->phrase
  "`chunks` should be a seq of numbers."
  [chunks]
  (->> (reduce (fn [v chunk]
                 (cond-xlet
                  :let [x (js/BigInt chunk)
                        s (-> (.toString x 16)
                              (str/upper-case))
                        n (count s)]
                  (even? n) (conj v s)
                  :else (conj v
                              (pad-left s (inc n)))))
               []
               chunks)
       (apply str)))
;; (js/console.log (chunks->phrase [298 104 105]))
