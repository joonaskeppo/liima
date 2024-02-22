(ns liima.rewrite-test
  (:require [clojure.test :refer [deftest is testing]]
            [rewrite-clj.zip :as z]
            [liima.rewrite :refer [clean-meta guess-namespace]]))

(deftest test-clean-meta
  (testing "without side-effects"
    (let [clean-src "(def a 1)"
          pred      #(= "liima" (namespace %))
          no-fx     (fn handle! [zloc _] zloc)
          process   (comp z/string #(clean-meta % pred no-fx) z/of-string)]
      (is (= clean-src
             (process clean-src)
             (process "^:liima/thing (def a 1)")
             (process "^{:liima/thing 1 :liima/other :thing} (def ^:liima/thing a 1)"))))
    ;; should preserve any metadata that wasn't filtered out by `pred`
    (let [clean-src "^{:borked false} (def ^:leave-me-be a 1)"
          pred      #(= "liima" (namespace %))
          no-fx     (fn handle! [zloc _] zloc)
          process   (comp z/string #(clean-meta % pred no-fx) z/of-string)]
      (is (= clean-src
             (process clean-src)
             (process "^{:borked false :liima/key :value} (def ^:leave-me-be ^:liima/form a 1)")
             (process "^{ \n  :liima/ignore :me    :borked false  \n  :liima/key  :value   } (def ^:leave-me-be ^:liima/form a 1)"))))))

(deftest test-guess-namespace
  (is (nil? (-> (z/of-string "\n:nothing-to-see-here\n")
                (guess-namespace))))
  (is (= "some.namespace"
         (-> (z/of-string "(def thing (let [x 1] x))\n (ns\n\n\nsome.namespace)\n (do (something)) ")
             (guess-namespace)))))
