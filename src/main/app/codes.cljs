(ns app.codes
  (:require
   [clojure.string :as str]
   [shadow.resource :as rc]
   [app.macros :as mac :refer-macros [cond-xlet ->hash]]
   [app.utils :refer [bimap]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mouse->px [mouse]
  (* 50 mouse))

(defn px->mouse [px]
  (/ px 50))

(def var-subcmds
  {:commit "B0"
   :get-parameter "B1"
   :set-parameter "B2"
   :get-keymap "B3"
   :set-keymap "B4"})

(def var-params
  {:enable-serial-header {:code "01" :type :num-boolean :advanced true
                          :label ["Serial Header"
                                  [:span.red " (breaks dot i/o)"]]}
   :enable-serial-logging {:code "02" :type :num-boolean :advanced true
                           :label ["Serial Logging"
                                   [:span.red " (breaks dot i/o)"]]}
   :enable-serial-debugging {:code "03" :type :num-boolean :advanced true
                             :label ["Serial Debugging"
                                     [:span.red " (breaks dot i/o)"]]}
   :enable-serial-raw {:code "04" :type :num-boolean :advanced true
                       :label ["Serial Raw"
                               [:span.red " (???)"]]}
   :enable-serial-chord {:code "05" :type :num-boolean :advanced true
                         :label ["Serial Chord"]}
   :enable-serial-keyboard {:code "06" :type :num-boolean :advanced true
                            :label ["Serial Keyboard"]}
   :enable-serial-mouse {:code "07" :type :num-boolean :advanced true
                         :label ["Serial Mouse"]}
   :enable-usb-hid-keyboard {:code "11" :type :num-boolean :advanced true
                             :label ["USB HID Keyboard"]}
   :enable-character-entry {:code "12" :type :num-boolean
                            :label ["Character Entry Mode"]}

   :gui-ctrl-swap-mode {:code "13" :type :num-boolean :ccl-only true :advanced true
                        :label ["Swap GUI and Ctrl" [:br]
                                [:span.white " (CC Lite only)"]]}

   :key-scan-duration {:code "14" :type :ms :min 1 :max 20 :step 1
                       :defaults [["Universal: 2ms" 2]]
                       :label ["Key Scan Duration (ms)"]}
   :key-debounce-press-duration {:code "15" :type :ms :min 0 :max 50 :step 1
                                 :defaults [["CC1: 7ms" 7]
                                            ["CC Lite: 20ms" 20]]
                                 :label ["Key Debounce Press Duration (ms)"]}
   :key-debounce-release-duration {:code "16" :type :ms :min 0 :max 50 :step 1
                                   :defaults [["CC1: 7ms" 7]
                                              ["CC Lite: 20ms" 20]]
                                   :label ["Key Debounce Release Duration (ms)"]}
   :keyboard-output-character-microsecond-delays
   {:code "17" :type :us :min 0 :max 10240 :step 40
    :defaults [["Universal: 480μs" 480]]
    :label ["Character Output Delay (μs)"]}

   :enable-usb-hid-mouse {:code "21" :type :num-boolean :advanced true
                          :label ["USB HID Mouse"]}
   :slow-mouse-speed {:code "22" :type :mouse :min 1 :max 100 :step 1
                      :defaults [["Universal: 5" 5]]
                      :label ["Slow Mouse Speed" [:br] [:span.gray "(1 unit = 50px)"]]}
   :fast-mouse-speed {:code "23" :type :mouse :min 1 :max 200 :step 1
                      :defaults [["Universal: 25" 25]]
                      :label ["Fast Mouse Speed" [:br] [:span.gray "(1 unit = 50px)"]]}
   :enable-active-mouse {:code "24" :type :num-boolean
                         :label ["Active Mouse" [:br] [:span.gray "(wiggle every minute)"]]}
   :mouse-scroll-speed {:code "25" :type :pos-int :min 1 :max 20 :step 1
                        :defaults [["Universal: 1" 1]]
                        :label ["Mouse Scroll Speed"]}
   :mouse-poll-duration {:code "26" :type :ms :min 1 :max 100 :step 1
                         :defaults [["Universal: 20ms" 20]]
                         :label ["Mouse Poll Duration (ms)"]}

   :enable-chording {:code "31" :type :num-boolean
                     :label ["Chording"]}
   :enable-chording-character-counter-timeout
   {:code "32" :type :num-boolean
    :label ["Chording Character Counter Timeout"]}
   :chording-character-counter-timeout-timer
   {:code "33" :type :ds :min 0 :max 255 :step 1
    :defaults [["Universal: 40ds" 40]]
    :label ["Chording Character Counter Timeout" [:br] "(deciseconds)"]}
   :chord-detection-press-tolerance {:code "34" :type :ms :min 1 :max 50 :step 1
                                     :defaults [["Universal: 25ms" 25]]
                                     :label ["Chord Detection Press Tolerance (ms)"]}
   :chord-detection-release-tolerance {:code "35" :type :ms :min 1 :max 50 :step 1
                                       :defaults [["Universal: 25ms" 25]]
                                       :label ["Chord Detection Release Tolerance (ms)"]}

   :enable-spurring {:code "41" :type :num-boolean
                     :label ["Spurring"]}
   :enable-spurring-character-counter-timeout
   {:code "42" :type :num-boolean
    :label ["Spurring Character Counter Timeout"]}
   :spurring-character-counter-timeout-timer
   {:code "43" :type :s :min 0 :max 240 :step 1
    :defaults [["Universal: 240s" 240]]
    :label ["Spurring Character Counter Timeout" [:br]
            "(seconds)"]}

   :enable-arpeggiates {:code "51" :type :num-boolean
                        :label ["Arpeggiates"]}
   :arpeggiate-tolerance {:code "54" :type :ms :min 100 :max 2000 :step 100
                          :defaults [["Universal: 800ms" 800]]
                          :label ["Arpeggiate Timeout"]}

   :enable-compound-chording {:code "61" :type :num-boolean
                              :label ["Compound Chording"]}
   :compound-tolerance {:code "64" :type :ms :min 100 :max 5000 :step 100
                        :defaults [["Universal: 1500ms" 1500]]
                        :label ["Compound Tolerance"]}

   :led-brightness {:code "81" :ccl-only true
                    :type :non-neg-int :min 0 :max 50 :step 1
                    :defaults [["CC Lite: 5" 5]]
                    :label ["LED Brightness" [:br]
                            [:span.white " (CC Lite only)"] [:br]
                            [:span.gray " (> 5 may use too much power)"]]}
   :led-color-code {:code "82" :ccl-only true
                    :type :non-neg-int
                    :label ["LED Color Code" [:br]
                            [:span.white " (CC Lite only)"]]}
   :enable-led-key-highlight {:code "83" :ccl-only true
                              :type :num-boolean
                              :label ["LED Key Highlight" [:br]
                                      [:span.white " (CC Lite only)"]]}

   :operating-system {:code "91" :type :dropdown
                      :values
                      [["0" "Windows"]
                       ["1" "Mac"]
                       ["2" "Linux"]
                       ["3" "iOS"]
                       ["4" "Android"]
                       ["255" "Unknown"]]
                      :label ["Operating System"]}
   :enable-realtime-feedback {:code "92" :type :num-boolean
                              :label ["Realtime Feedback" [:br]
                                      [:span.gray "(mode switch messages typed out)"]]}
   :enable-charachorder-ready-on-startup {:code "93" :type :num-boolean
                                          :label ["Type Ready Message when Powered On"]}})
(def code->var-param
  (into {} (map (fn [[k {:as v :keys [code]}]]
                  [code (assoc v :param k)])
                var-params)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def keymap-code-json (-> (rc/inline "app/keymap_codes.json")
                          (js/JSON.parse)))
(def keymap-codes
  (->> keymap-code-json
       (map (fn [[code type action action-desc notes]]
              (let [code-int (js/parseInt code)]
                (->hash code code-int type action action-desc notes))))))

(def keymap-code-types (into #{} (map :type keymap-codes)))
(def longest-action-text
  (reduce (fn [prev x]
            (if (< (count prev) (count x))
              x prev))
          (->> (map :action keymap-codes) (sort))))

(def code->keymap-code
  (into {} (map (fn [{:as m :keys [code]}]
                  [code m])
                keymap-codes)))

(defn partition-when [pred xs]
  (reduce (fn [v x]
            (if (pred x)
              (conj v [x])
              (let [i (-> v count dec)]
                (update v i conj x))))
          [[]]
          xs))

(defn partition-when-too-big-or-pred [n pred xs]
  (reduce (fn [v x]
            (let [i (-> v count dec)
                  last-subv (get v i)]
              (cond
                (<= n (count last-subv)) (conj v [x])
                (pred x) (conj v [x])
                :else (update v i conj x))))
          [[]]
          xs))

(def ascii-keymap-codes
  (->> (filter #(= (:type %) "ASCII") keymap-codes)
       (sort-by :code-int)
       (partition-when #(contains? #{"!" "0" "A" "[" "a"} (:action %)))))

(def cp1252-keymap-codes
  (->> (filter #(= (:type %) "CP-1252") keymap-codes)
       (sort-by :code-int)
       (partition-when #(contains? #{"¦" "À" "à"} (:action %)))))

(def keyboard-keymap-codes
  (->> (filter #(= (:type %) "Keyboard") keymap-codes)
       (sort-by :code-int)
       (partition-when #(contains? #{"RIGHT_CTRL" "RELEASE_MOD"} (:action %)))))

(def mouse-keymap-codes
  (->> (filter #(= (:type %) "Mouse") keymap-codes)
       (sort-by :code-int)
       (partition-when #(contains? #{"MS_MOVE_RT" "MS_SCRL_RT"} (:action %)))))

(def charachorder-keymap-codes
  (->> (filter #(= (:type %) "CharaChorder") keymap-codes)
       (sort-by :code-int)
       (partition-when #(contains? #{"KM_1_L" "AMBILEFT"} (:action %)))))

(def charachorder-one-keymap-codes
  (->> (filter #(= (:type %) "CharaChorder One") keymap-codes)
       (sort-by :code-int)
       (partition-when #(contains? #{"LH_INDEX_3D" "RH_THUMB_3_3D" "RH_INDEX_3D"} (:action %)))))

(def raw-keymap-codes
  (->> (filter #(= (:type %) "Raw Scancode") keymap-codes)
       (sort-by :code-int)
       (partition-when-too-big-or-pred
        19
        #(contains? #{"KEY_A" "KEY_N" "KEY_1" "ENTER" "F1" "PRTSCN" "KP_1" "F13" "EXECUTE"
                      "INTL1" "KSC_99"}
                    (:action %)))))

(def none-keymap-codes
  (->> (filter #(= (:type %) "None") keymap-codes)
       (sort-by :code-int)
       (partition-when-too-big-or-pred
        20
        #(contains? #{} (:action %)))))
