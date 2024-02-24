(ns liima.docs
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
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
  [registry s]
  (let [templates (read-templates s)]
    (reduce (fn [s [template ttype [block-id & block-args]]]
              (let [f           (get recognized-resolvers ttype)
                    block-opts  (into {} (mapv vec (partition 2 block-args)))
                    replacement (if (seq block-opts)
                                  (f registry block-id block-opts)
                                  (f registry block-id))]
                (str/replace-first s template replacement)))
            s
            templates)))
