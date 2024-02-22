(ns liima.test-util
  (:require [liima.blocks :refer [sync-registry-with-string!]]))

(defn make-registry [strs]
  (let [!registry (atom {})]
    (doseq [s strs]
      (sync-registry-with-string! !registry s))
    !registry))
