# liima

A tool for gluing snippets of your Clojure code into your docs.
Inspired by literate programming.

Based on `rewrite-clj`.

## Why?

Code is hardly ever optimized for pedagogy.
Unlike org-babel, WEB, or noweb, most literate-programming-inspired tools I know of are lacking in one crucial aspect: the sheer difficulty of "out-of-order" presentation of ideas (from the perspective of the resultant code).
So, I wanted to experiment and try out something that:
- would provide the bare minimum for creating out-of-order documentation using fragments of actual code
- wouldn't cause noticeable changes in workflow

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

and you wanted to include that snippet as-is in your documentation, so that the `println` form gets replaced with `<<println>>`.
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

Next, add the following in a Markdown file:

````md
Here's a neat function:

```
@{{content :my.ns/my-fn :my.ns/print "<<println>>"}}
```

where `<<println>>` is: `@{{content :my.ns/print}}`

````

Then, evaluate:
```
(liima.docs/process-notes! {:docs-root "/path/to/docs" :output-root "./target/docs"})
```

A version of the original Markdown file has been dumped under `./target/docs` with the template replaced:

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

For content replacements, currently only `content` and `ns` (for attempted inference of the code block's namespace) are supported.
Links can be replaced by appending the typical Markdown notation with `@`, like so:

```
@[My Link](./linked-page.md)
```

which would become

```
[My Link](./linked-page/index.html)
```

## Limitations

- All `:liima/ref` values must be either unqualified, or fully qualified, keywords (i.e., can't use `::keyword` sugar).
- Only code that [supports metadata](https://clojure.org/reference/metadata) can be tagged
