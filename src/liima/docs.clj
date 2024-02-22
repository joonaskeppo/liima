(ns liima.docs
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [liima.core]))

(def ^:private recognized-resolvers
  {"content" liima.core/resolve-content
   "ns"      (constantly "TODO")})

(def ^:private re-template
  (re-pattern
    (->> (map first recognized-resolvers)
         (str/join "|")
         (format "\\{\\{@(%s)\\s+([^\\{^\\}]+)\\}\\}"))))

(defn- get-templates
  "Find instances of Liima templates in string `s`."
  [s]
  (->> (re-seq re-template s)
       (map (fn [v]
              (update v 2 #(edn/read-string (format "[%s]" %)))))))

(defn replace-templates
  "Find instances of Liima templates in string `s`,
  and replace them using the appropriate fns using content in `registry`."
  [registry s]
  (let [templates (get-templates s)]
    (reduce (fn [s [template ttype [block-id & block-args]]]
              (let [f           (get recognized-resolvers ttype)
                    block-opts  (into {} (mapv vec (partition 2 block-args)))
                    replacement (if (seq block-opts)
                                  (f registry block-id block-opts)
                                  (f registry block-id))]
                (str/replace-first s template replacement)))
            s
            templates)))

