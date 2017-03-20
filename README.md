# troublems

This project highlights a bug in om in which the shared state is not propagated
correctly when building multiple components in a for-loop (or `om/build-all`) 
and using advanced optimisation. 

So, building with `scripts/release` the bug will appear for versions om-0.9.0 

This is broken for om-0.9.0 until om-1.0.0-alpha17, and the fix seems to have 
appeared in 1.0.0-alpha18.

The diff between the two version:

```diff
diff --git a/project.clj b/project.clj
index 169aa46..e0b854f 100644
--- a/project.clj
+++ b/project.clj
@@ -1,4 +1,4 @@
-(defproject org.omcljs/om "1.0.0-alpha17"
+(defproject org.omcljs/om "1.0.0-alpha18-SNAPSHOT"
   :description "ClojureScript interface to Facebook's React"
   :url "http://github.com/swannodette/om"
   :license {:name "Eclipse"
diff --git a/src/devcards/om/devcards/core.cljs b/src/devcards/om/devcards/core.cljs
index b8fdbb0..b5e7c94 100644
--- a/src/devcards/om/devcards/core.cljs
+++ b/src/devcards/om/devcards/core.cljs
@@ -211,3 +211,21 @@
 (defcard test-counters-atom
   (om/app-state counters-reconciler))
 
+;; -----------------------------------------------------------------------------
+;; Children
+
+(defui Children
+  Object
+  (render [this]
+    (dom/div nil
+      (map identity
+        #js [(dom/div nil "Foo")
+             (dom/div nil "Bar")
+             (map identity
+               #js [(dom/div nil "Bar")
+                    (dom/div nil "Woz")])]))))
+
+(def children (om/factory Children))
+
+(defcard test-lazy-children
+  (children))
\ No newline at end of file
diff --git a/src/main/om/dom.clj b/src/main/om/dom.clj
index 5cd1005..a3189b1 100644
--- a/src/main/om/dom.clj
+++ b/src/main/om/dom.clj
@@ -130,7 +130,8 @@
 
 (defn ^:private gen-react-dom-inline-fn [tag]
   `(defmacro ~tag [opts# & children#]
-     `(~'~(symbol "js" (str "React.DOM." (name tag))) ~opts# ~@children#)))
+     `(~'~(symbol "js" (str "React.DOM." (name tag))) ~opts#
+        ~@(clojure.core/map (fn [x#] `(om.util/force-children ~x#)) children#))))
 
 (defmacro ^:private gen-react-dom-inline-fns []
   `(do
@@ -140,7 +141,9 @@
 
 (defn ^:private gen-react-dom-fn [tag]
   `(defn ~tag [opts# & children#]
-     (.apply ~(symbol "js" (str "React.DOM." (name tag))) nil (cljs.core/into-array (cons opts# children#)))))
+     (.apply ~(symbol "js" (str "React.DOM." (name tag))) nil
+       (cljs.core/into-array
+         (cons opts# (cljs.core/map om.util/force-children children#))))))
 
 (defmacro ^:private gen-react-dom-fns []
   `(do
diff --git a/src/main/om/dom.cljs b/src/main/om/dom.cljs
index 143b53c..220db3a 100644
--- a/src/main/om/dom.cljs
+++ b/src/main/om/dom.cljs
@@ -3,6 +3,7 @@
   (:require-macros [om.dom :as dom])
   (:require [cljsjs.react]
             [cljsjs.react.dom]
+            [om.util :as util]
             [goog.object :as gobj]))
 
 (dom/gen-react-dom-fns)
diff --git a/src/main/om/next.cljs b/src/main/om/next.cljs
index 32600ee..7b100c3 100644
--- a/src/main/om/next.cljs
+++ b/src/main/om/next.cljs
@@ -8,6 +8,7 @@
             [om.next.protocols :as p]
             [om.next.impl.parser :as parser]
             [om.next.cache :as c]
+            [om.util :as util]
             [clojure.zip :as zip])
   (:import [goog.debug Console]))
 
@@ -267,7 +268,7 @@
                :omcljs$shared     *shared*
                :omcljs$instrument *instrument*
                :omcljs$depth      *depth*}
-          children))))))
+           (util/force-children children)))))))
 
 (defn ^boolean component?
   "Returns true if the argument is an Om component."
diff --git a/src/main/om/util.cljs b/src/main/om/util.cljs
new file mode 100644
index 0000000..7aa1a71
--- /dev/null
+++ b/src/main/om/util.cljs
@@ -0,0 +1,5 @@
+(ns om.util)
+
+(defn force-children [x]
+  (cond->> x
+    (seq? x) (into [] (map force-children))))
```

To reproduce 

run `scripts/release` and open `index_release.html`
