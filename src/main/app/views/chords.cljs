(ns app.views.chords 
  (:require [app.codes :refer [code-int->short-dom]]
            [app.macros :refer-macros [->hash cond-xlet]]
            [app.serial.constants :refer [get-port]]
            [app.utils :refer [parse-binary-chord-string]]))

(defn chord-chunks-com [chunks]
  (let [chunks (filter #(not= 0 %) chunks)]
    [:div {:class "chord-chunks mh2"}
     (for [chunk chunks] 
       [:div {:class "chord-chunks__chunk"} (code-int->short-dom chunk)])]))

(defn chords-view [{:as args :keys [port-id]}]
  (let [port (get-port port-id)
        {:keys [*binary-chord-string]} port
        binary-chord-string @*binary-chord-string
        {:keys [chunks]} (when binary-chord-string
                           (parse-binary-chord-string binary-chord-string))]
    [:div {:class "pa3 pb0"}
     [:div.f2
      [:div.dib "Last Chord: "]
      [chord-chunks-com chunks]]
     ]))
