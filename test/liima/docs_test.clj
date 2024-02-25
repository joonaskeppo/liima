(ns liima.docs-test
  (:require [clojure.test :refer [deftest is testing]]
            [liima.test-util :refer [make-registry]]
            [liima.docs :refer [replace-templates]]))

(deftest test-replace-templates
  (let [registry  (make-registry ["^{:liima/ref :outer} (def ^{:liima/ref :inner} a 1)"])]
    (testing "with simple replacement"
      (let [text "Here's some\ntext with templates: {{@content :outer :inner \"<<redacted>>\"}} {{@content :inner}} ok bye"]
        (is (= "Here's some\ntext with templates: (def <<redacted>> 1) a ok bye"
               (replace-templates registry text)))))
    (testing "with quoted brackets" ;; quotes should be escaped
      (let [text "something {{@content :outer :inner \"{{boom?}}\"}}"]
        (is (= "something (def {{boom?}} 1)"
               (replace-templates registry text)))))))
