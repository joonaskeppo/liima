(ns liima.docs-test
  (:require [clojure.test :refer [deftest is testing]]
            [liima.test-util :refer [make-registry]]
            [liima.docs :refer [replace-references]]))

(deftest test-replace-references
  (let [registry  (make-registry ["^{:liima/ref :outer} (def ^{:liima/ref :inner} a 1)"])]
    (testing "with content replacement, simple"
      (let [text "Here's some\ntext with templates: @{{content :outer :inner \"<<redacted>>\"}} @{{content :inner}} ok bye"]
        (is (= "Here's some\ntext with templates: (def <<redacted>> 1) a ok bye"
               (replace-references registry text)))))
    (testing "with content replacement, quoted brackets" ;; quotes should be escaped
      (let [text "something @{{content :outer :inner \"{{boom?}}\"}}"]
        (is (= "something (def {{boom?}} 1)"
               (replace-references registry text)))))
    (testing "with link replacement"
      (let [text "This is a @[test link](./linked.md)."]
        (is (= "This is a [test link](./linked/index.html)."
               (replace-references registry text)))))))

