(ns liima.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [liima.test-util :refer [make-registry]]
            [liima.core :refer [resolve-content]]))

(deftest test-resolve-content
  (testing "with nested reference"
    (let [!registry (make-registry ["^{:liima/ref :outer} (def ^{:liima/ref :inner} a 1)"])]
      (is (= "(def <elided> 1)" (resolve-content @!registry :outer {:inner "<elided>"}))))))
