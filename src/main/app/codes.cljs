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
  {:enable-serial-header {:code "01" :type :num-boolean}
   :enable-serial-logging {:code "02" :type :num-boolean}
   :enable-serial-debugging {:code "03" :type :num-boolean}
   :enable-serial-raw {:code "04" :type :num-boolean}
   :enable-serial-chord {:code "05" :type :num-boolean}
   :enable-serial-keyboard {:code "06" :type :num-boolean}
   :enable-serial-mouse {:code "07" :type :num-boolean}
   :enable-usb-hid-keyboard {:code "11" :type :num-boolean}
   :enable-character-entry {:code "12" :type :num-boolean :ccl-only true}

   :gui-ctrl-swap-mode {:code "13" :type :num-boolean :ccl-only true}

   :key-scan-duration {:code "14" :type :ms}
   :key-debounce-press-duration {:code "15" :type :ms}
   :key-debounce-release-duration {:code "16" :type :ms}

   :keyboard-output-character-microsecond-delays {:code "17" :type :us}

   :enable-usb-hid-mouse {:code "21" :type :num-boolean}
   :slow-mouse-speed {:code "22" :type :mouse}
   :fast-mouse-speed {:code "23" :type :mouse}
   :enable-active-mouse {:code "24" :type :num-boolean}
   :mouse-scroll-speed {:code "25" :type :pos-int}
   :mouse-poll-duration {:code "26" :type :ms}

   :enable-chording {:code "31" :type :num-boolean}
   :enable-chording-character-counter-timeout {:code "32" :type :num-boolean}
   :chording-character-counter-timeout-timer {:code "33" :type :ds}
   :chord-detection-press-tolerance {:code "34" :type :ms}
   :chord-detection-release-tolerance {:code "35" :type :ms}

   :enable-spurring {:code "41" :type :num-boolean}
   :enable-spurring-character-counter-timeout {:code "42" :type :num-boolean}
   :spurring-character-counter-timeout-timer {:code "43" :type :s}

   :enable-arpeggiates {:code "51" :type :num-boolean}
   :arpeggiate-tolerance {:code "54" :type :ms}

   :enable-compound-chording {:code "61" :type :num-boolean}
   :compound-tolerance {:code "64" :type :ms}

   :led-brightness {:code "81" :ccl-only true
                    :type :non-neg-int :max 50}
   :led-color-code {:code "82" :ccl-only true
                    :type :non-neg-int :disabled true}
   :enable-led-key-highlight {:code "83" :ccl-only true
                              :type :num-boolean :disabled true}

   :operating-system {:code "91" :type :non-neg-int
                      :values (bimap
                               {"Windows" "0"
                                "Mac" "1"
                                "Linux" "2"
                                "iOS" "3"
                                "Android" "4"
                                "Unknown" "255"})}
   :enable-realtime-feedback {:code "92" :type :num-boolean}
   :enable-charachorder-ready-on-startup {:code "93" :type :num-boolean}})
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
