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
