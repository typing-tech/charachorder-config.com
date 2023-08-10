(ns hooks.binding-block
  (:require [clj-kondo.hooks-api :as api :refer [list-node vector-node map-node keyword-node
                                                 token-node string-node
                                                 keyword-node? list-node?]]))

(defn dump [node] (prn (api/sexpr node)))

(defn rewrite [let-mode forms]
  ;; (prn forms)
  (cond
    ;; no form is nil
    (= (count forms) 0) (token-node 'nil)

    ;; one thing remaining is the return value
    (= (count forms) 1)
    (first forms)

    (list-node? (first forms))
    (let [[x & xs] forms]
      (list-node
       (list (token-node 'do)
             x
             (rewrite let-mode xs))))

    ;; keyword special detected
    (keyword-node? (first forms))
    (let [[_ x & xs] forms
          [k a b & cs] forms]
      (case (:k k)
        (:let :plet :pplet) (rewrite let-mode (rest forms))
        (:do :pdo) (list-node
                    (list (token-node 'do)
                          x
                          (rewrite let-mode xs)))
        (:when :return-when) (list-node
                              (list (token-node 'if) a
                                    b
                                    (rewrite let-mode cs)))
        (:with-open) (list-node
                      (list (token-node 'clojure.core/with-open)
                            (vector-node [a b])
                            (rewrite let-mode cs)))
        (token-node 'nil)))

    ;; collect let forms and recurse
    :else
    (let [[let-forms remaining-forms]
          (loop [let-forms []
                 remaining-forms forms]
            (if (<= (count remaining-forms) 1)
              [let-forms remaining-forms]
              (let [[a b & xs] remaining-forms]
                (if-not (or (keyword-node? a) (list-node? a))
                  (recur (conj let-forms a b)
                         xs)
                  [let-forms remaining-forms]))))]
      (if (< 0 (count let-forms))
        (list-node
         (list (token-node 'let)
               (vector-node let-forms)
               (rewrite let-mode remaining-forms)))
        (rewrite let-mode remaining-forms)))))

(defn binding-block [{:as m :keys [node cljc lang filename config ns context]}]
  (let [forms (-> node :children rest)
        final-node (apply rewrite forms)]
    ;; (dump final-node)
    {:node final-node}))

(defn bb [{:as m :keys [node cljc lang filename config ns context]}]
  (let [forms (-> node :children rest)
        final-node (rewrite :let forms)]
    ;; (dump final-node)
    {:node final-node}))
