(require '[cljs.build.api :as b])

(b/watch "src"
  {:main 'troublems.core
   :output-to "out/troublems.js"
   :output-dir "out"})
