(ns liima.blocks-test
  (:require [clojure.test :refer [deftest is testing]]
            [liima.blocks :refer [resolve-content sync-registry-with-string!]]))

(defn make-registry [strs]
  (let [!registry (atom {})]
    (doseq [s strs]
      (sync-registry-with-string! !registry s))
    !registry))

(deftest test-resolve-content
  (testing "with nested reference"
    (let [!registry (make-registry ["^{:liima/ref :outer} (def ^{:liima/ref :inner} a 1)"])]
      (is (= "(def <elided> 1)" (resolve-content !registry :outer {:inner "<elided>"}))))))
