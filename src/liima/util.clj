(ns liima.util
  "Generic helper fns")

(defn map-vals
  "Like `map`, but applied only to the values of associative structure `m`"
  [f m]
  (->> m
       (map (fn [[k v]] [k (f v)]))
       (into {})))

(defn select-keys-by
  "Like `select-keys`, but keys selected via predicate `f`."
  [f m]
  (->> m
       (filter (fn [[k v]] (when (f k) [k v])))
       (into {})))
