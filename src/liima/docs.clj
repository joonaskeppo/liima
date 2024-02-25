(ns liima.docs
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [liima.fs]
            [liima.core]))

(def ^:private recognized-resolvers
  {"content" liima.core/resolve-content
   "ns"      (fn [reg block-id] (get-in reg [block-id :presumed-ns]))})

(defn open-brackets? [s]
  (str/starts-with? s "{{"))

(defn close-brackets? [s]
  (str/starts-with? s "}}"))

(defn- quote? [[ch]] (= \" ch))

(defn- read-templates
  "Find instances of Liima templates in string `s`"
  ([s]
   (read-templates [] s))
  ([acc s]
   (cond
     (open-brackets? s)
     (let [[_ resolver-type]  (re-find #"^\{\{@(ns|content)" s)
           [templ s]          (when resolver-type
                                (loop [templ "" s s]
                                  (cond
                                    (close-brackets? s)
                                    [(str templ (subs s 0 2)) (subs s 2)]

                                    (quote? s)
                                    (if-let [s* (re-find #"^\".*\"" s)]
                                      (recur (str templ s*) (subs s (count s*)))
                                      (throw
                                       (ex-info "Expected closing \" character inside template"
                                                {:template (subs s 0 20)})))

                                    (empty? s)
                                    (throw
                                     (ex-info "Expected closing }} for template"
                                              {:template (subs s 0 20)}))

                                    :else
                                    (recur (str templ (first s)) (subs s 1)))))
           templ-args         (->> (subs templ (count (format "{{@%s" resolver-type)) (- (count templ) 2))
                                   (format "[%s]")
                                   edn/read-string)]
       (if templ
         (recur (conj acc [templ resolver-type templ-args]) s)
         (recur acc s)))

     (empty? s)
     acc

     :else
     (recur acc (subs s 1)))))

(defn replace-templates
  "Find instances of Liima templates in string `s`,
  and replace them using the appropriate fns using content in `registry`."
  [!registry s]
  (let [templates (read-templates s)]
    (reduce (fn [s [template ttype [block-id & block-args]]]
              (let [f           (get recognized-resolvers ttype)
                    block-opts  (into {} (mapv vec (partition 2 block-args)))
                    replacement (if (seq block-opts)
                                  (f !registry block-id block-opts)
                                  (f !registry block-id))]
                (str/replace-first s template replacement)))
            s
            templates)))

;; process:
;; 1. from `path-to-docs`, we read in all files recursively
;; 2. if manifest.edn found, process it -> configuration
;; 3. for all .md files found, slurp them, and process them -> spit to output dir (e.g., some-note.md -> target/docs/some-note/index.html)
;; links like [./some-note.md](Some Note) could be automatically handled (./some-note/index.html)

;; TODO: `@` as generic "deref" symbol (e.g., @{{content ...}} -> content or @[./thing.md](Thing) -> updated link)

(defn process-docs!
  "Process all documents, optionally via `opts`.
  `opts` is a configuration map of the following:
  - `project-root`:           string, path to project root (default: \".\")
  - `docs-root`:              string, path to docs root (default: \"./docs\")
  - `output-root`:            string, path to output dir for processed docs (default: \"./target/docs/\")
  - `make-file-output-path`:  fn, takes in seq of path relative to `docs-root`, and returns output file path (default: `last`)
  - `compile-doc`:            fn, takes in content of doc as string, returns compiled doc as string (default: identity)"
  {:arglists '([] [opts])}
  ([] (process-docs! {}))
  ([{:keys [docs-root compile-doc output-root make-file-output-path project-root]
     :or {project-root            "."
          docs-root               "./docs"
          output-root             "./target/docs"
          make-file-output-path   last
          compile-doc             identity}}]
   (liima.fs/make-directory output-root)
   (let [!registry             (liima.core/make-registry-for-project project-root)
         {:keys [manifest md]} (liima.fs/find-docs-files docs-root)]
     (doseq [note-file md
             :let [note-content (slurp note-file)
                   output-path  (->> (liima.fs/get-relative-path-seq docs-root note-file)
                                     make-file-output-path
                                     (liima.fs/combine-paths output-root))]]
       (->> note-content
            (replace-templates !registry)
            compile-doc
            (spit output-path))))))

(comment
  (process-docs!))
