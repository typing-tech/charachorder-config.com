(ns app.serial.ops
  (:require [app.db :refer [*db]]
            [app.hw :refer [get-hw-layers+sorted-switch-key-ids
                            get-hw-switch-keys]]
            [app.macros :refer-macros [->hash cond-xlet]]
            [app.ratoms :refer [*active-port-id *num-devices-connected]]
            [app.serial.constants :refer [*ports baud-rates dummy-port-id
                                          get-port]]
            [app.serial.fns :as fns :refer [gen-cml-get-chordmap-by-index-fn
                                            query-all-var-keymaps!]]
            [app.utils :refer [lex-comp-numbers hex-chord-string->sorted-chunks]] 
            [cljs.core.async :as async
             :refer [<! >! chan close! onto-chan! put!]
             :refer-macros [go go-loop]]
            [datascript.core :as ds]
            [posh.reagent :as posh :refer [transact!]]))

(defn disconnect! [port-id]
  (let [{:keys [close-port-and-cleanup!]} (get-port port-id)]
    (reset! *active-port-id nil)
    (close-port-and-cleanup!)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn refresh-param [port-id param]
  (let [port (get-port port-id)]
    (fns/query-var-param! port param)))

(defn refresh-params [port-id]
  (let [port (get-port port-id)]
    (fns/query-all-var-params! port)))

(defn set-param! [port-id param raw-value]
  (assert (keyword? param))
  (let [{:as port :keys [fn-ch]} (get-port port-id)
        cmd (fns/cmd-var-set-parameter param raw-value)]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
                (js/console.log "SEND" cmd)
                (>! write-ch cmd)
                (let [ret (<! read-ch)
                      {:keys [success]} (fns/parse-var-set-parameter-ret ret)]
                  (js/console.log "RECV" ret)
                  (if success
                    (js/console.log "set parameter success")
                    (js/console.error "set parameter ERROR"))
                  (refresh-param port-id param))))]
      (put! fn-ch f))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reset-restart! [port-id]
  (let [{:keys [close-port-and-cleanup! fn-ch]} (get-port port-id)]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
                (>! write-ch "RST RESTART")
                (let [ret (<! read-ch)]
                  (js/console.log ret))
                (reset! *active-port-id nil)
                (close-port-and-cleanup!)))]
      (put! fn-ch f))))

(defn reset-bootloader! [port-id]
  (let [{:keys [close-port-and-cleanup! fn-ch]} (get-port port-id)]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
                (>! write-ch "RST BOOTLOADER")
                (let [ret (<! read-ch)]
                  (js/console.log ret))
                (reset! *active-port-id nil)
                (close-port-and-cleanup!)))]
      (put! fn-ch f))))

(defn reset-params! [port-id]
  (let [{:keys [close-port-and-cleanup! fn-ch]} (get-port port-id)]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
                (>! write-ch "RST PARAMS")
                (let [ret (<! read-ch)]
                  (js/console.log ret))
                (reset! *active-port-id nil)
                (close-port-and-cleanup!)))]
      (put! fn-ch f))))

(defn factory-reset! [port-id]
  (let [{:keys [close-port-and-cleanup! fn-ch]} (get-port port-id)
        cmd "RST FACTORY"]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
                (>! write-ch "RST FACTORY")
                (let [ret (<! read-ch)]
                  (js/console.log ret))
                (reset! *active-port-id nil)
                (close-port-and-cleanup!)))]
      (put! fn-ch f))))

(defn reset-keymaps! [port-id]
  (let [{:keys [close-port-and-cleanup! fn-ch]} (get-port port-id)]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
                (>! write-ch "RST KEYMAPS")
                (let [ret (<! read-ch)]
                  (js/console.log ret))
                (reset! *active-port-id nil)
                (close-port-and-cleanup!)))]
      (put! fn-ch f))))

(defn reset-starter! [port-id]
  (let [{:keys [close-port-and-cleanup! fn-ch]} (get-port port-id)]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
                (>! write-ch "RST STARTER")
                (let [ret (<! read-ch)]
                  (js/console.log ret))
                (reset! *active-port-id nil)
                (close-port-and-cleanup!)))]
      (put! fn-ch f))))

(defn reset-func! [port-id]
  (let [{:keys [close-port-and-cleanup! fn-ch]} (get-port port-id)]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
                (>! write-ch "RST FUNC")
                (let [ret (<! read-ch)]
                  (js/console.log ret))
                (reset! *active-port-id nil)
                (close-port-and-cleanup!)))]
      (put! fn-ch f))))

(defn reset-clearcml! [port-id]
  (let [{:keys [close-port-and-cleanup! fn-ch]} (get-port port-id)]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
                (>! write-ch "RST CLEARCML")
                (let [ret (<! read-ch)]
                  (js/console.log ret))
                (reset! *active-port-id nil)
                (close-port-and-cleanup!)))]
      (put! fn-ch f))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn commit!
  "Returns nil."
  [port-id]
  (let [{:as port :keys [close-port-and-cleanup! fn-ch]} (get-port port-id)
        layers+sorted-switch-key-ids (get-hw-layers+sorted-switch-key-ids port)

        cmd (fns/cmd-var-commit)

        m (ds/pull @*db '[*] [:port/id port-id])
        attr-nses (map (fn [[layer switch-key-ids]]
                         (str layer "." switch-key-ids))
                       layers+sorted-switch-key-ids)
        txs (reduce (fn [txs attr-ns]
                      (let [a (get m (keyword attr-ns "code"))
                            hw-code-key (keyword attr-ns "hw.code")
                            b (get m hw-code-key)]
                        (if (not= a b)
                          (conj txs [:db/add [:port/id port-id] hw-code-key a])
                          txs)))
                    []
                    attr-nses)]
    ; (js/console.log txs)
    (letfn [(f [{:keys [write-ch read-ch read-to-serial-log!]}]
              (go
                (>! write-ch cmd)
                (let [ret (<! read-ch)
                      {:keys [success]} (fns/parse-commit-ret ret)]
                  ;; (js/console.log ret)
                  (if success
                    (read-to-serial-log! "COMMIT success")
                    (read-to-serial-log! "COMMIT ERROR"))
                  (when success (transact! *db txs))
                  success)))]
      (put! fn-ch f))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-keymap! [port-id layer switch-key-id code]
  (assert layer)
  (assert (string? switch-key-id))
  (assert code)
  (assert (string? switch-key-id))
  (let [{:as port :keys [fn-ch]} (get-port port-id)
        switch-keys (get-hw-switch-keys port)
        location (get-in switch-keys [switch-key-id :location])
        cmd (fns/cmd-var-set-keymap layer location code)]
    (letfn [(f [{:keys [write-ch read-ch]}]
              (go
                (js/console.log "SEND" cmd)
                (>! write-ch cmd)
                (let [ret (<! read-ch)
                      {:keys [success]} (fns/parse-var-set-keymap-ret ret)]
                  (js/console.log "RECV" ret)
                  (if success
                    (js/console.log "set keymap success")
                    (js/console.error "set keymap ERROR")))))]
      (put! fn-ch f))))

(defn refresh-keymaps-after-commit! [port-id]
  (assert port-id)
  (let [{:as port} (get-port port-id)]
    (query-all-var-keymaps! port :boot true)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn store-chord-count! [{:as port :keys [port-id write-ch read-ch *num-chords]}]
  (let [cmd (fns/cmd-cml-get-chordmap-count)]
    (go
      (>! write-ch cmd)
      (let [ret (<! read-ch)
            {:keys [success count]} (fns/parse-cml-get-chordmap-count-ret ret)]
        (when success
          (reset! *num-chords count)
          (transact! *db [[:db/add [:port/id port-id] :port/chord-count count]]))))))

(defn sort+add-chords-to-db! [port-id *chord-sorting-method chords]
  (let [sort-method @*chord-sorting-method]
    (->> chords
         (map (fn [{:as m
                    hex-chord-string :chord/hex-chord-string}]
                (assoc m :chord-chunks (hex-chord-string->sorted-chunks hex-chord-string))))
         (sort-by (fn [{:keys [chord-chunks]}]
                    (lex-comp-numbers chord-chunks)))
         (map-indexed (fn [index m]
                        ;; index 0 reserved for new chord
                        (assoc m :chord/index (inc index))))
         (vec)
         (transact! *db))))

(defn query-all-chordmaps! [port]
  (let [compute-and-queue-chord-reads!
        (fn [{:as port :keys [port-id fn-ch *num-chords]}]
          (let [get-chordmap-fns (map gen-cml-get-chordmap-by-index-fn (range @*num-chords))
                begin! (fn [{:keys [*is-reading-chords *chords *chord-read-index]}]
                               (go
                                 (reset! *chord-read-index 0)
                                 (reset! *chords (transient []))
                                 (reset! *is-reading-chords true)))
                end! (fn [{:keys [*is-reading-chords *chords *chord-sorting-method]}]
                       (go
                         (let [chords (persistent! @*chords)]
                           (sort+add-chords-to-db! port-id *chord-sorting-method chords))
                         (reset! *is-reading-chords false)))
                fns (concat [begin!]
                            get-chordmap-fns
                            [end!])]
            (onto-chan! fn-ch fns false)))]
    (go
      (<! (store-chord-count! port))
      (compute-and-queue-chord-reads! port))))

(defn simple-delete-chord!
  "This always fails due to a bug in the firmware."
  [port-id hex-chord-string]
  (let [{:as port :keys [fn-ch]} (get-port port-id)
        cmd (fns/cmd-cml-del-chordmap-by-chord hex-chord-string)]
    (letfn [(f [{:as p :keys [write-ch read-ch]}]
              (go
                (>! write-ch cmd)
                (let [ret (<! read-ch)
                      {:keys [success]} (fns/parse-cml-del-chordmap-by-chord-ret ret)]
                  (when success
                    (let [chord [:chord/id [port-id hex-chord-string]]]
                      (transact! *db [[:db/retractEntity chord]]))))))]
      (put! fn-ch f))))

(defn delete-chord!
  [port-id hex-chord-string cb]
  (let [{:keys [fn-ch]} (get-port port-id)
        del-cmd (fns/cmd-cml-del-chordmap-by-chord hex-chord-string)
        read-cmd (fns/cmd-cml-get-chordmap-by-chord hex-chord-string)]
    (letfn [(f [{:as p :keys [write-ch read-ch]}]
              (go
                (cond-xlet
                 :do (>! write-ch del-cmd)
                 ;; as of CCOS 1.0.2 this always reports failure,
                 ;; but the chord is probably deleted
                 :let [_ret (<! read-ch)]
                 :do (>! write-ch read-cmd)
                 :let [ret (<! read-ch)
                       {:keys [success]} (fns/parse-cml-get-chordmap-by-chord-ret ret)]
                 ;; :do (js/console.log m)
                 ;; if confirmed deleted, remove from db
                 (not success) (when cb (cb))
                 :return nil)))]
      (put! fn-ch f))))

(defn read-chord!
  [port-id hex-chord-string]
  (let [{:keys [fn-ch]} (get-port port-id)
        cmd (fns/cmd-cml-get-chordmap-by-chord hex-chord-string)]
    (letfn [(f [{:as p :keys [write-ch read-ch]}]
              (go
                (>! write-ch cmd)
                (let [ret (<! read-ch)
                      {:as m :keys [success]} (fns/parse-cml-get-chordmap-by-chord-ret ret)]
                  (js/console.log m))))]
      (put! fn-ch f))))

(defn set-chord!
  [port-id hex-chord-string phrase cb]
  (let [{:keys [fn-ch]} (get-port port-id)
        set-cmd (fns/cmd-cml-set-chordmap-by-chord hex-chord-string phrase)
        read-cmd (fns/cmd-cml-get-chordmap-by-chord hex-chord-string)]
    (letfn [(f [{:as p :keys [write-ch read-ch]}]
              (go
                (>! write-ch set-cmd)
                ;; as of CCOS 1.0.2 this always reports failure,
                ;; but the chord is probably set
                (<! read-ch)
                (>! write-ch read-cmd)
                (let [{:as m :keys [success]} (fns/parse-cml-get-chordmap-by-chord-ret (<! read-ch))]
                  (js/console.log m)
                  (js/console.log phrase)
                  (when (and success
                             ;; there is a bug in the firmware that causes the phrase to be
                             ;; wrong/truncated when high actions (two bytes) are used
                             ;; (= phrase (:phrase m))
                             true)
                    (when cb (cb))))))]
      (put! fn-ch f))))
