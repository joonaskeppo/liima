(ns liima.rewrite
  "Helpers for working with rewrite-clj"
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n])
  (:refer-clojure :exclude [map?]))

(defn meta?
  "Is zipper `zloc` at a meta tag?"
  [zloc]
  (= :meta (z/tag zloc)))

(defn map?
  "Is zipper `zloc` at a map tag?"
  [zloc]
  (= :map (z/tag zloc)))

(defn drop-meta
  "If zipper `zloc` is at a meta tag, drop it.
  Returns updated zipper."
  [zloc]
  (if (meta? zloc)
    (z/replace zloc (-> zloc z/down z/right z/node))
    zloc))

(defn remove-keys
  "If zipper `zloc` is at a map node, remove keys `ks` from the map.
  Returns the updated zipper."
  [zloc ks]
  (if-not (map? zloc)
    zloc
    (let [ks            (set ks)
          node          (z/node zloc)
          new-children  (let [children  (vec (n/children node))
                              max-idx   (dec (count children))
                              match?    #(and (some? %) (n/sexpr-able? %) (contains? ks (n/sexpr %)))]
                          (loop [acc              []
                                 k-or-v-removals  0
                                 idx              0
                                 clean-whitespace false]
                            (let [child     (get children idx)
                                  removing  (not= 0 (mod k-or-v-removals 2))]
                              (cond
                                (> idx max-idx)
                                acc

                                (and (n/whitespace? child) removing)
                                (recur acc k-or-v-removals (inc idx) true) 

                                (or removing (match? child))
                                (recur acc (inc k-or-v-removals) (inc idx) true)

                                ;; If we just previously removed something,
                                ;; we are in "cleaning whitespace" mode, to not leave any trailing whitespace
                                (and (n/whitespace? child) clean-whitespace)
                                (recur acc k-or-v-removals (inc idx) true)

                                ;; If the next non-whitespace token is removable,
                                ;; then we shouldn't conj all that leading whitespace.
                                ;; Since newlines are different tokens from regular whitespace,
                                ;; we need to prune them all.
                                (n/whitespace? child)
                                (let [remainder         (drop idx children)
                                      blanks            (take-while n/whitespace? remainder)
                                      next-non-ws-child (first (drop-while n/whitespace? remainder))]
                                  (if (match? next-non-ws-child)
                                    (recur acc (inc k-or-v-removals) (inc (+ idx (count blanks))) true)
                                    (recur (into acc blanks) k-or-v-removals (+ idx (count blanks)) false)))

                                :else
                                (recur (conj acc child) k-or-v-removals (inc idx) false)))))]
      (->> (n/replace-children node new-children)
           (z/replace zloc)))))

(defn clean-meta
  "Clean selected metadata from code via zipper postwalk.
  Pick metadata with `pred`, using `handle!` for side-effectful processing.
  `pred` takes a metadata key and returns a truthy value if it is to be selected.
  `handle!` takes a cleaned version of the zipper, and kv-pairs matching `pred`.
  The return value of `handle!` should return the updated zipper value.
  Returns the processed zipper at root."
  [zloc pred handle!]
  (z/postwalk zloc meta?
              (fn visit [zloc]
                (let [childloc (z/down zloc)]
                  (if-not (map? childloc)
                    ;; like ^:my/meta
                    (let [meta-k (z/sexpr childloc)]
                      (if (seq (filter pred (list meta-k)))
                        (let [next-zloc (drop-meta zloc)]
                          (handle! next-zloc {meta-k true}))
                        zloc))
                    ;; like ^{:my/meta true :other :stuff}
                    (let [full-meta-map     (z/sexpr childloc)
                          selection         (->> full-meta-map keys (filter pred))
                          selected-meta-map (select-keys full-meta-map selection)]
                      (cond
                        (empty? selected-meta-map)
                        zloc

                        (= full-meta-map selected-meta-map)
                        (let [next-zloc (drop-meta zloc)]
                          (handle! next-zloc selected-meta-map))

                        :else
                        (let [next-zloc (z/up (remove-keys childloc selection))]
                          (handle! next-zloc selected-meta-map)))))))))

;; TODO: improve on this
(defn guess-namespace
  "Find next ns form's name from current zipper location"
  [zloc]
  (->> zloc
       (iterate z/right)
       (take-while (complement z/end?))
       (some (fn [zloc]
               (when (= 'ns (or (some-> zloc z/down z/down z/node :value)
                                (some-> zloc z/down z/node :value)))
                 (or (some-> zloc z/down z/down z/right z/node :string-value)
                     (some-> zloc z/down z/right z/node :string-value)))))))

(comment
  (-> (z/of-string "^{:liima/thing 1 :liima/other :thing} (def ^:liima/thing a 1)")
      drop-meta
      z/string)

  (-> (z/of-string "^{:liima/thing 1 :liima/other :thing} (def ^:liima/thing a 1)")
      (clean-meta #(= "liima" (namespace %)) (constantly nil))
      z/string))
