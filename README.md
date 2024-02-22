# liima

A one-trick-pony utility for gluing modified snippets of Clojure code into your docs, or wherever.

Based on `rewrite-clj`.

## A Contrived Example

Supposing you had the following code on your classpath:

```clj
(defn my-fn
  "An example fn for demonstrative purposes"
  []
  (println "hello world!")
  (+ 1 1))
```

and you wanted to "glue" that function into some string or piece of documentation, so that the `println` form gets replaced with `<<println>>`.
First, add some metadata:

```clj
^{:liima/ref :my.ns/my-fn}
(defn my-fn
  "An example fn for demonstrative purposes"
  []
  ^{:liima/ref :my.ns/print}
  (println "hello world!")
  (+ 1 1))
```

Now we can create a registry of all liima references on the classpath, and use that to generate the desired string, like so:

```clj
(require '[liima.core :as liima])

(def registry (liima/make-registry-from-classpath))

(liima/resolve-content registry :my.ns/my-fn {:my.ns/print "<<println>>"})
```

As a result of the `resolve-content` call, we get a stringified version of that original fn with the `println` form replaced:
```clj
"(defn my-fn
  \"An example fn for demonstrative purposes\"
  []
  <<println>>
  (+ 1 1))"
```

What if we just wanted to grab that `println`?
Easy:

```clj
(liima/resolve-content registry :my.ns/print {})    ; just return the stringified `println` form; nothing to replace
(liima/resolve-content registry :my.ns/print)       ; same thing
```

## Why?

Good question.

- I had a rough idea for a super minimal, literate-programming-adjacent utility after trying out the infinitely more interesting [Clerk](https://github.com/nextjournal/clerk).
- I was bored, and it was the weekend.

You know how it goes.

## Limitations

- All `:liima/ref` values must be either unqualified, or fully qualified, keywords (i.e., can't use `::keyword` sugar).
- Only code that [supports metadata](https://clojure.org/reference/metadata) can be tagged
