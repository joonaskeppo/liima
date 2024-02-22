(ns liima.core
  "Definitions and tools related to code blocks"
  (:require [clojure.string :as str]
            [rewrite-clj.zip :as z]
            [rewrite-clj.node.protocols :as node.protocols]
            [liima.rewrite]
            [liima.fs])
  (:import [java.io File]))

;; We can't simply stringify our refs, as calling `string` on a parent node would give us
;; something like `"(def my-thing \"the-ref\")"` instead of `"(def my-thing the-ref)"`.
;; And we can't call `sexpr` either, as that would just hide our metadata maps when we want to refer to things.
;; In other words, we would only get an approximation of our original code (sans `:liima/ref` and potential friends).
;; We need a custom Node type that emits a string representation that can easily be regex'd.
;; So something like: <<liima-ref:ref-uuid>>.
;; Using a UUID instead of the ref name means the likelihood of a clash with a string literal that
;; contains that specific representation is practically negligible, making it _appear_ like less of a hack.
;; Just in case.

(def ^:private ^:const block-regex
  #"<<liima-block:([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})>>")

(defn- make-block-str [uuid]
  (format "<<liima-block:%s>>" uuid))

(defrecord Block [block-id uuid content presumed-ns]
  node.protocols/Node
  (tag [_node] ::block)
  (node-type [_node] ::block)
  (printable-only? [_node] true)
  (sexpr* [node _opts] node)
  (length [_node] (count content))
  (string [_node] (make-block-str uuid))
  
  node.protocols/NodeCoerceable
  (coerce [form] form))

(defn upsert-block!
  "Upserts a new Block into `!registry` under `block-id`.
  Creates, then assocs, a Block object with `block-id` and `content`.
  Returns created Block."
  [!registry presumed-ns block-id content]
  (let [uuid  (str (random-uuid))
        block (->Block block-id uuid content presumed-ns)]
    (swap! !registry assoc block-id block)
    block))

(defn replace-block
  "Replace references of `block-id` in string `s` with `replacement`.
   `block-id` must be a unique entry within `!registry`."
  [registry s block-id replacement]
  (let [uuid (get-in registry [block-id :uuid])]
    (str/replace s (make-block-str uuid) replacement)))

(defn find-blocks
  "Find all unique block ids in string `s` using `!registry`.
  Block ids are expected to be found as unique entries in the registry."
  [registry s]
  (when-let [uuid->block-id (some->> registry vals (map (juxt :uuid :block-id)) (into {}))]
    (some->> (re-seq block-regex s)
             (map (comp uuid->block-id second))
             set)))

(defn resolve-content
  "Resolve the content in a block using `block-id` and optional `replacements` via `registry`.
  `replacements` should be a map of block ids to content strings.
  If the parent content string contains block references not included in `replacements`,
  the referenced blocks' original content strings will be used (as found in the registry)."
  ([registry block-id]
   (resolve-content registry block-id {}))
  ([registry block-id replacements]
   (let [content               (get-in registry [block-id :content])
         blocks-needed         (find-blocks registry content)
         default-replacements  (->> registry
                                    (keep (fn [[k v]]
                                            (when (contains? blocks-needed k)
                                              [k (:content v)])))
                                    (into {}))
         replacements          (select-keys (merge default-replacements replacements) blocks-needed)]
     (if (seq replacements)
       (reduce (fn [acc [ref-name content]]
                 (replace-block registry acc ref-name content))
               content
               replacements)
       content))))

(def ^:private liima-keyword?
  "Keyword associated with this library?"
  (comp #(= "liima" %) namespace))

(defn- handle-block-upsert-with-zipper!
  "Handle upserting a block to `!registry` from zipper `zloc` and metadata `m`.
  `presumed-ns` is a best-effort guess at the namespace of the block.
  Returns updated zipper value."
  [!registry presumed-ns zloc m]
  (if-let [block-id (:liima/ref m)]
    (let [block (upsert-block! !registry presumed-ns block-id (z/string zloc))]
      (z/replace zloc block))
    zloc))

(defn sync-registry-with-string!
  [!registry ^String s]
  (let [zloc        (z/of-string* s)
        presumed-ns (liima.rewrite/guess-namespace zloc)
        upsert!     (partial handle-block-upsert-with-zipper! !registry presumed-ns)]
    (liima.rewrite/clean-meta zloc liima-keyword? upsert!))
  @!registry)

(defn sync-registry-with-file!
  [!registry ^File file]
  (let [zloc        (z/of-file* file)
        presumed-ns (liima.rewrite/guess-namespace zloc)
        upsert!     (partial handle-block-upsert-with-zipper! !registry presumed-ns)]
    (liima.rewrite/clean-meta zloc liima-keyword? upsert!))
  @!registry)

(defn make-registry-from-files
  "Create a registry of blocks using `files`.
  Returns registry."
  [files]
  (let [!registry (atom {})]
    (doseq [file files]
      (sync-registry-with-file! !registry file))
    @!registry))

(def make-registry-from-classpath
  "Create a registry of blocks using classpath.
  Returns registry."
  (comp make-registry-from-files liima.fs/find-classpath-files))

