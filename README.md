# liima

A one-trick-pony utility for gluing modified snippets of Clojure code into your docs, or wherever.

Based on `rewrite-clj`.

## Why?

Code is hardly ever optimized for pedagogy.
Unlike org-babel, WEB, or noweb, most literate-programming-inspired tools I know of are lacking in one crucial aspect: the sheer difficulty of "out-of-order" presentation of ideas (from the perspective of the resultant code).
So, I wanted to experiment and try out something that:
- would provide the bare minimum for creating out-of-order documentation using fragments of actual code, and that
- wouldn't cause noticeable changes in workflow to any of my coworkers.

## Usage

### A Contrived Example

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

Now we can create a registry of all Liima references on the classpath, and use that to generate the desired string, like so:

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

### Templating

Liima has some super basic templating helpers under `liima.docs`.
Using the example from the previous section, we could write Markdown like so:

````md
Here's a neat function:

```
{{@content :my.ns/my-fn :my.ns/print "<<println>>"}}
```

where `<<println>>` is: `{{@content :my.ns/print}}`

````

Instead of using `liima.core/resolve-content`, we could use `liima.docs/replace-templates`:

```clj
(liima.docs/replace-templates registry markdown-string)
```

and we would get:

````md
Here's a neat function:

```
(defn my-fn
  "An example fn for demonstrative purposes"
  []
  <<println>>
  (+ 1 1))
```

where `<<println>>` is: `(println "hello world!")`
````

Currently, only `@content` and `@ns` (for attempted inference of the code block's namespace) are supported.

## Limitations

- All `:liima/ref` values must be either unqualified, or fully qualified, keywords (i.e., can't use `::keyword` sugar).
- Only code that [supports metadata](https://clojure.org/reference/metadata) can be tagged
