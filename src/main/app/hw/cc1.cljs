(ns app.hw.cc1)

;; hand
;; L - left
;; R - right

;; fingers
;; 0 - Thumb
;; 1 - Index
;; 2 - Middle
;; 3 - Ring
;; 4 - Pinky

;; [hand]-[finger]-[switch num]
;; several switches, like the thumb have multiple switches

(def switch-keys
  {;; left thumb bottom/palm
   "lt2d" {:location "0"}
   "lt2e" {:location "1"}
   "lt2n" {:location "2"}
   "lt2w" {:location "3"}
   "lt2s" {:location "4"}

   ;; left thumb middle
   "lt1d" {:location "5"}
   "lt1e" {:location "6"}
   "lt1n" {:location "7"}
   "lt1w" {:location "8"}
   "lt1s" {:location "9"}

   ;; left thumb top
   "lt0d" {:location "10"}
   "lt0e" {:location "11"}
   "lt0n" {:location "12"}
   "lt0w" {:location "13"}
   "lt0s" {:location "14"}

   ;; left index top
   "li0d" {:location "15"}
   "li0e" {:location "16"}
   "li0n" {:location "17"}
   "li0w" {:location "18"}
   "li0s" {:location "19"}

   ;; left middle top
   "lm0d" {:location "20"}
   "lm0e" {:location "21"}
   "lm0n" {:location "22"}
   "lm0w" {:location "23"}
   "lm0s" {:location "24"}

   ;; left ring top
   "lr0d" {:location "25"}
   "lr0e" {:location "26"}
   "lr0n" {:location "27"}
   "lr0w" {:location "28"}
   "lr0s" {:location "29"}

   ;; left pinky top
   "lp0d" {:location "30"}
   "lp0e" {:location "31"}
   "lp0n" {:location "32"}
   "lp0w" {:location "33"}
   "lp0s" {:location "34"}

   ;; left middle arrow
   "lm1d" {:location "35"}
   "lm1e" {:location "36"}
   "lm1n" {:location "37"}
   "lm1w" {:location "38"}
   "lm1s" {:location "39"}

   ;; left ring mouse
   "lr1d" {:location "40"}
   "lr1e" {:location "41"}
   "lr1n" {:location "42"}
   "lr1w" {:location "43"}
   "lr1s" {:location "44"}

   ;; right thumb bottom/palm
   "rt2d" {:location "45"}
   "rt2w" {:location "46"}
   "rt2n" {:location "47"}
   "rt2e" {:location "48"}
   "rt2s" {:location "49"}

   ;; right thumb middle
   "rt1d" {:location "50"}
   "rt1w" {:location "51"}
   "rt1n" {:location "52"}
   "rt1e" {:location "53"}
   "rt1s" {:location "54"}

   ;; right thumb top
   "rt0d" {:location "55"}
   "rt0w" {:location "56"}
   "rt0n" {:location "57"}
   "rt0e" {:location "58"}
   "rt0s" {:location "59"}

   ;; right index top
   "ri0d" {:location "60"}
   "ri0w" {:location "61"}
   "ri0n" {:location "62"}
   "ri0e" {:location "63"}
   "ri0s" {:location "64"}

   ;; right middle top
   "rm0d" {:location "65"}
   "rm0w" {:location "66"}
   "rm0n" {:location "67"}
   "rm0e" {:location "68"}
   "rm0s" {:location "69"}

   ;; right ring top
   "rr0d" {:location "70"}
   "rr0w" {:location "71"}
   "rr0n" {:location "72"}
   "rr0e" {:location "73"}
   "rr0s" {:location "74"}

   ;; right pinky top
   "rp0d" {:location "75"}
   "rp0w" {:location "76"}
   "rp0n" {:location "77"}
   "rp0e" {:location "78"}
   "rp0s" {:location "79"}

   ;; right middle arrow
   "rm1d" {:location "80"}
   "rm1w" {:location "81"}
   "rm1n" {:location "82"}
   "rm1e" {:location "83"}
   "rm1s" {:location "84"}

   ;; right ring mouse
   "rr1d" {:location "85"}
   "rr1w" {:location "86"}
   "rr1n" {:location "87"}
   "rr1e" {:location "88"}
   "rr1s" {:location "89"}})

(def location->switch-key-id
  (into {} (map (fn [[k {:keys [location]}]] [location k])
                switch-keys)))
