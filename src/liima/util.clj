(ns liima.util)

(defn atom?
  "Is `x` an atom?"
  [x]
  (instance? clojure.lang.Atom x))
