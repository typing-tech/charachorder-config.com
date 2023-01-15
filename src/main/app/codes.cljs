(ns app.codes)

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
   :enable-character-entry {:code "12" :type :num-boolean}

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
   :enable-chording-character-counter-killer {:code "32" :type :num-boolean}
   :chording-character-counter-killer-timer {:code "33" :type :ds}
   :chord-detection-press-tolerance {:code "34" :type :ms}
   :chord-detection-release-tolerance {:code "35" :type :ms}

   :enable-spurring {:code "41" :type :num-boolean}
   :enable-spurring-character-counter-killer {:code "42" :type :num-boolean}
   :spurring-character-counter-killer-timer {:code "43" :type :s}

   :enable-arpeggiates {:code "51" :type :num-boolean}
   :arpeggiate-tolerance {:code "54" :type :ds}

   :enable-compound-chording {:code "61" :type :num-boolean}
   :compound-tolerance {:code "64" :type :ds}

   :led-brightness {:code "81" :ccl-only true
                    :type :non-neg-int :max 50}
   :led-color-code {:code "82" :ccl-only true
                    :type :non-neg-int :disabled true}
   :enable-led-key-highlight {:code "83" :ccl-only true
                              :type :num-boolean :disabled true}

   :operating-system {:code "91" :type :non-neg-int
                      :values-map {"Windows" "0"
                                   "Mac" "1"
                                   "Linux" "2"
                                   "iOS" "3"
                                   "Android" "4"
                                   "Unknown" "255"}}
   :enable-realtime-feedback {:code "92" :type :num-boolean}
   :enable-charachorder-ready-on-startup {:code "93" :type :num-boolean}})
