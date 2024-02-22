(ns liima.docs-test
  (:require [clojure.test :refer [deftest is]]
            [liima.test-util :refer [make-registry]]
            [liima.docs :refer [replace-templates]]))

(deftest test-replace-templates
  (let [registry  @(make-registry ["^{:liima/ref :outer} (def ^{:liima/ref :inner} a 1)"])
        text      "Here's some\ntext with templates: {{@content :outer :inner \"<<redacted>>\"}} {{@content :inner}} ok bye"]
    (is (= "Here's some\ntext with templates: (def <<redacted>> 1) a ok bye"
           (replace-templates registry text)))))
