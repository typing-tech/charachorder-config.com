(ns app.macros
  (:require
   [promesa.core :as p]))

(defmacro current-ns []
  (str *ns*))

(defmacro ->hash [& vars]
  (list `zipmap
    (mapv keyword vars)
    (vec vars)))

(defmacro args
  "Converts (args a b c) -> (assoc args :a a :b b :c c)"
  [& vars]
  (let [xs (interleave (mapv keyword vars)
                       (vec vars))]
    `(assoc ~'args ~@xs)))

(defmacro cond-xlet
  "An alternative to `clojure.core/cond` where instead of a test/expression pair,
  it is possible to also have:
  :do form  - unconditionally execute `form`, useful for printf style debugging or logging
  :let []   - standard let binding vector pair
  :plet []  - promesa.core/let binding vector pair - uses Promises, chops up stack traces
  :pplet [] - promesa.core/plet binding vector pair -  uses Promises, chops up stack traces
  
  Try to use :let if you know that a function call result is synchronous."
  [& clauses]
  (cond (empty? clauses)
        nil

        (not (even? (count clauses)))
        (throw (ex-info (str `cond-xlet " requires an even number of forms")
                        {:form &form
                         :meta (meta &form)}))

        :else
        (let [[test expr-or-binding-form & more-clauses] clauses]
          (cond
            (= :let test) `(let ~expr-or-binding-form (cond-xlet ~@more-clauses))
            (= :plet test) `(p/let ~expr-or-binding-form (cond-xlet ~@more-clauses))
            (= :pplet test) `(p/plet ~expr-or-binding-form (cond-xlet ~@more-clauses))
            (= :do test) `(when true ~expr-or-binding-form (cond-xlet ~@more-clauses))
            (= :pdo test) `(p/do ~expr-or-binding-form (cond-xlet ~@more-clauses))
           ;; standard case
            :else `(if ~test
                     ~expr-or-binding-form
                     (cond-xlet ~@more-clauses))))))

(defmacro log+time [msg expr]
  `(let [start# (cljs.core/system-time)
         ret# ~expr]
     (prn (cljs.core/str ~msg " "
                         (.toFixed (- (cljs.core/system-time) start#) 6)
                         " msecs"))
     ret#))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmacro binding-block [let-mode & body]
  (cond
    ;; no form is nil
    (= (count body) 0) nil

    ;; one thing remaining is the return value
    (= (count body) 1)
    (first body)

    (list? (first body))
    (let [[x & xs] body]
      `(do ~x
           (binding-block ~let-mode ~@xs)))

    ;; keyword special detected
    (keyword? (first body))
    (let [[_ x & xs] body
          [k a b & cs] body]
      (case k
        (:let :plet :pplet) `(binding-block ~k ~@(rest body))
        :do `(do ~x
                 (binding-block ~let-mode ~@xs))
        :pdo `(promesa.core/do
                ~x
                (binding-block ~let-mode ~@xs))
        (:when :return-when) `(if ~a
                                ~b
                                (binding-block ~let-mode ~@cs))
        :with-open `(clojure.core/with-open [~a ~b]
                      (binding-block ~let-mode ~@cs))))

    ;; collect let forms and recurse
    :else
    (let [[let-forms remaining-body]
          (loop [let-forms []
                 remaining-body body]
            (if (<= (count remaining-body) 1)
              [let-forms remaining-body]
              (let [[a b & xs] remaining-body]
                (if-not (or (keyword? a) (list? a))
                  (recur (conj let-forms a b)
                         xs)
                  [let-forms remaining-body]))))

          -let (condp = let-mode
                 :let 'clojure.core/let
                 :plet 'promesa.core/let
                 :pplet 'promesa.core/pplet)]
      (if (< 0 (count let-forms))
        `(~-let [~@let-forms]
          (binding-block ~let-mode ~@remaining-body))
        `(binding-block ~let-mode ~@remaining-body)))))

(defmacro bb [& body]
  `(binding-block :let ~@body))
