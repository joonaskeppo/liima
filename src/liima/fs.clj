(ns liima.fs
  "Filesystem helpers"
  (:require [clojure.java.classpath :as cp])
  (:import [java.io File]))

(defn find-classpath-files
  "Retrieve seq of files under classpath"
  []
  (mapcat (fn find-files-in-directory [^File dir]
            (reduce (fn [acc ^File v]
                      (if (.isDirectory v)
                        (into acc (find-files-in-directory v))
                        (conj acc v)))
                    []
                    (.listFiles dir)))
          ;; per https://github.com/nextjournal/clerk/blob/main/src/nextjournal/clerk/classpath.clj
          (filter #(.isDirectory ^File %) (cp/system-classpath))))
