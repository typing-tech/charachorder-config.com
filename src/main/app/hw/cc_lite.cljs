(ns app.hw.cc-lite)

(def switch-keys
  {;; bottom row
   "lgui" {:location "0"}
   "lctrl" {:location "1" :u 1.25}
   "lalt" {:location "2" :u 1.25}
   "lspace" {:location "3" :u 2}
   "lfn" {:location "4"}
   "ralt" {:location "5"}
   "rspace" {:location "6" :u 2}
   "rfn" {:location "7" :u 1.25}
   "rgui" {:location "8" :u 1.25}
   "left" {:location "9"}
   "down" {:location "10"}
   "right" {:location "11"}
   ;; zxc row
   "lshift" {:location "12" :u 2}
   "z" {:location "13"}
   "x" {:location "14"}
   "c" {:location "15"}
   "v" {:location "16"}
   "b" {:location "17"}
   "n" {:location "18"}
   "m" {:location "19"}
   "comma" {:location "20"}
   "period" {:location "21"}
   "slash" {:location "22"}
   "rshift" {:location "23"}
   "up" {:location "24"}
   "del" {:location "25"}
   ;; asd row
   "capslock" {:location "26" :u 1.75}
   "a" {:location "27"}
   "s" {:location "28"}
   "d" {:location "29"}
   "f" {:location "30"}
   "g" {:location "31"}
   "h" {:location "32"}
   "j" {:location "33"}
   "k" {:location "34"}
   "l" {:location "35"}
   ";" {:location "36"}
   "'" {:location "37"}
   "enter" {:location "38" :u 2.25}
   ;; qwe row
   "tab" {:location "39" :u 1.5}
   "q" {:location "40"}
   "w" {:location "41"}
   "e" {:location "42"}
   "r" {:location "43"}
   "t" {:location "44"}
   "y" {:location "45"}
   "u" {:location "46"}
   "i" {:location "47"}
   "o" {:location "48"}
   "p" {:location "49"}
   "[" {:location "50"}
   "]" {:location "51"}
   "bslash" {:location "52" :u 1.5}
   ;; number row
   "esc" {:location "53" :u 1}
   "1" {:location "54"}
   "2" {:location "55"}
   "3" {:location "56"}
   "4" {:location "57"}
   "5" {:location "58"}
   "6" {:location "59"}
   "7" {:location "60"}
   "8" {:location "61"}
   "9" {:location "62"}
   "0" {:location "63"}
   "hypen" {:location "64"}
   "equal" {:location "65"}
   "bs" {:location "66" :u 2}})

(def location->switch-key-id
  (into {} (map (fn [[k {:keys [location]}]] [location k])
                switch-keys)))
(def sorted-switch-keys-by-loc
  (->> switch-keys
       (sort-by #(-> % val :location js/parseInt))
       (map key)))

(def layers+sorted-switch-key-ids
  (for [layer ["A1" "A2" "A3"]
        switch-key-id sorted-switch-keys-by-loc]
    [layer switch-key-id]))
