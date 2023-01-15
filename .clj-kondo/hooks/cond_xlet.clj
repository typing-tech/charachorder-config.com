(ns hooks.cond-xlet
  (:require [clj-kondo.hooks-api :as api]))

(defn unroll [[a b] xs]
  (cond
    (and (nil? a) (nil? b))
    (api/token-node 'nil)

    (contains? #{:let :plet :pplet} (:k a))
    (api/list-node (list (api/token-node 'let)
                         b
                         (unroll (take 2 xs) (drop 2 xs))))

    (contains? #{:do :pdo} (:k a))
    (api/list-node (list (api/token-node 'when)
                         (api/token-node 'true)
                         b
                         (unroll (take 2 xs) (drop 2 xs))))
    (contains? #{:return-if} (:k a))
    (let [sym (gensym "cond-xlet-return-if-val-")]
      (api/list-node (list (api/token-node 'if-let)
                           (api/vector-node [(api/token-node sym) b])
                           (api/token-node sym)
                           (unroll (take 2 xs) (drop 2 xs)))))

    :else
    (api/list-node (list (api/token-node 'if)
                         a
                         b
                         (unroll (take 2 xs) (drop 2 xs))))))

(defn cond-xlet [{:keys [node]}]
  (let [pairs (rest (:children node))
        ret (unroll (take 2 pairs) (drop 2 pairs))]
    ;(println (api/sexpr ret))
    {:node ret}))
