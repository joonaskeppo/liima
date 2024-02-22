(ns liima.fs
  "Filesystem helpers"
  (:require [clojure.java.classpath :as cp])
  (:import [java.io File]))

(defn- clj-file?
  "Check that file extension is that of a Clojure file.
  Supported cases: .clj .cljs .cljr .cljc .edn"
  [^File v]
  (re-find #"\.(clj|cljs|cljr|cljc|edn)$" (.getName v)))

(defn find-classpath-files
  "Retrieve seq of files under classpath"
  []
  (mapcat (fn find-files-in-directory [^File dir]
            (reduce (fn [acc ^File v]
                      (cond
                        (.isDirectory v)  (into acc (find-files-in-directory v))
                        (clj-file? v)     (conj acc v)
                        :else             acc))
                    []
                    (.listFiles dir)))
          ;; per https://github.com/nextjournal/clerk/blob/main/src/nextjournal/clerk/classpath.clj
          (filter #(.isDirectory ^File %) (cp/system-classpath))))
