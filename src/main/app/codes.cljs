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
  {:enable-serial-header {:code "01" :type :num-boolean
                          :label ["Serial Header"
                                  [:span.red " (breaks dot i/o)"]]}
   :enable-serial-logging {:code "02" :type :num-boolean
                           :label ["Serial Logging"
                                   [:span.red " (breaks dot i/o)"]]}
   :enable-serial-debugging {:code "03" :type :num-boolean
                             :label ["Serial Debugging"
                                     [:span.red " (breaks dot i/o)"]]}
   :enable-serial-raw {:code "04" :type :num-boolean
                       :label ["Serial Raw"
                               [:span.red " (???)"]]}
   :enable-serial-chord {:code "05" :type :num-boolean
                         :label ["Serial Chord"]}
   :enable-serial-keyboard {:code "06" :type :num-boolean
                            :label ["Serial Keyboard"]}
   :enable-serial-mouse {:code "07" :type :num-boolean
                         :label ["Serial Mouse"]}
   :enable-usb-hid-keyboard {:code "11" :type :num-boolean
                             :label ["USB HID Keyboard"]}
   :enable-character-entry {:code "12" :type :num-boolean
                            :label ["Character Entry Mode"]}

   :gui-ctrl-swap-mode {:code "13" :type :num-boolean :ccl-only true
                        :label ["Swap GUI and Ctrl" [:br]
                                [:span.white " (CC Lite only)"]]}

   :key-scan-duration {:code "14" :type :ms
                       :label ["Key Scan Duration"]}
   :key-debounce-press-duration {:code "15" :type :ms
                                 :label ["Key Debounce Press Duration"]}
   :key-debounce-release-duration {:code "16" :type :ms
                                   :label ["Key Debounce Release Duration"]}
   :keyboard-output-character-microsecond-delays {:code "17" :type :us
                                                  :label ["Character Output Delay (μs)"]}

   :enable-usb-hid-mouse {:code "21" :type :num-boolean
                          :label ["USB HID Mouse"]}
   :slow-mouse-speed {:code "22" :type :mouse
                      :label ["Slow Mouse Speed"]}
   :fast-mouse-speed {:code "23" :type :mouse
                      :label ["Fast Mouse Speed"]}
   :enable-active-mouse {:code "24" :type :num-boolean
                         :label ["Active Mouse" [:br] [:span.gray "(wiggle every minute)"]]}
   :mouse-scroll-speed {:code "25" :type :pos-int
                        :label ["Mouse Scroll Speed"]}
   :mouse-poll-duration {:code "26" :type :ms
                         :label ["Mouse Poll Duration"]}

   :enable-chording {:code "31" :type :num-boolean
                     :label ["Chording"]}
   :enable-chording-character-counter-timeout
   {:code "32" :type :num-boolean
    :label ["Chording Character Counter Timeout"]}
   :chording-character-counter-timeout-timer 
   {:code "33" :type :ds
    :label ["Chording Character Counter Timeout (deciseconds)"]}
   :chord-detection-press-tolerance {:code "34" :type :ms
                                     :label ["Chord Detection Press Tolerance"]}
   :chord-detection-release-tolerance {:code "35" :type :ms
                                       :label ["Chord Detection Release Tolerance"]}

   :enable-spurring {:code "41" :type :num-boolean
                     :label ["Spurring"]}
   :enable-spurring-character-counter-timeout {:code "42" :type :num-boolean
                                               :label ["Spurring Character Counter Timeout"]}
   :spurring-character-counter-timeout-timer {:code "43" :type :s
                                              :label ["Spurring Character Counter Timeout (seconds)"]}

   :enable-arpeggiates {:code "51" :type :num-boolean
                        :label ["Arpeggiates"]}
   :arpeggiate-tolerance {:code "54" :type :ms
                          :label ["Arpeggiate Tolerance"]}

   :enable-compound-chording {:code "61" :type :num-boolean
                              :label ["Compound Chording"]}
   :compound-tolerance {:code "64" :type :ms
                        :label ["Compound Tolerance"]}

   :led-brightness {:code "81" :ccl-only true
                    :type :non-neg-int :max 50
                    :label ["LED Brightness" [:br]
                            [:span.white " (CC Lite only)"]]}
   :led-color-code {:code "82" :ccl-only true
                    :type :non-neg-int :disabled true
                    :label ["LED Color Code" [:br]
                            [:span.white " (CC Lite only)"]]}
   :enable-led-key-highlight {:code "83" :ccl-only true
                              :type :num-boolean :disabled true
                              :label ["LED Key Highlight" [:br]
                                      [:span.white " (CC Lite only)"]]}

   :operating-system {:code "91" :type :non-neg-int
                      :values (bimap
                               {"Windows" "0"
                                "Mac" "1"
                                "Linux" "2"
                                "iOS" "3"
                                "Android" "4"
                                "Unknown" "255"})
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
