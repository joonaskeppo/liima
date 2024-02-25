(ns liima.fs
  "Filesystem helpers"
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io File]))

(defn- clj-file?
  "Check that file extension is that of a Clojure file.
  Supported cases: .clj .cljs .cljr .cljc .edn"
  [^File v]
  (re-find #"\.(clj|cljs|cljr|cljc|edn)$" (.getName v)))

(defn- md-file?
  "Check that file extension is that of a Markdown file."
  [^File v]
  (re-find #"\.md$" (.getName v)))

(defn- manifest-file?
  "Is file manifest.edn?"
  [^File v]
  (= "manifest.edn" (.getName v)))

(def find-files
  "Find files under path"
  (comp file-seq io/file))

(defn find-clj-files
  "Find clj files under presumed project directory (or \".\").
  Optionally, find files under specified `path` string.
  `path` may be relative or absolute."
  ([]
   (find-clj-files "."))
  ([path]
   (filter clj-file? (find-files path))))

(defn find-docs-files
  "Find Markdown notes and possible manifest.edn under `path`"
  [path]
  (let [files (find-files path)]
    {:manifest  (some (fn [^File v] (when (manifest-file? v) v)) files)
     :md        (filter md-file? files)}))

(defn combine-paths
  "Combine path strings in sequential order.
  Example: (combine-paths \"/\" \"/hello\" \"/world.md\") ; => \"/hello/world.md\""
  [& paths]
  (->> paths
       (map #(str/replace % #"^/" ""))
       (apply io/file)
       .getCanonicalPath))

(defn make-directory
  "Simple wrapper over `clojure.java.io/make-parents`
  that ensures all directories are made through `path`"
  [path]
  (io/make-parents (combine-paths path "/dummy")))

(defn get-relative-path-seq
  "Get path seq of `descendant` relative to `root`"
  [root descendant]
  (let [root-canonical        (.getCanonicalPath (io/file root))
        descendant-canonical  (.getCanonicalPath (io/file descendant))]
    (-> (str/replace descendant-canonical root-canonical "")
        (str/split #"/")
        rest)))
