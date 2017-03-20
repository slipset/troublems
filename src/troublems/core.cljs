(ns troublems.core
  (:require [clojure.browser.repl :as repl]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

;; (defonce conn
;;   (repl/connect "http://localhost:9000/repl"))

(enable-console-print!)

(println "Hello world!")


(defonce state (atom {:data "data"
                      :shared {:flux "foo"}}))


(defn li-component [[n] owner]
  (reify
    om/IRender
    (render [_]
      (dom/ul nil (str n " " (om/get-shared owner :flux))))))

(defn my-component [data]
  (om/component
   (dom/ul nil
          (for [i (range 0 3)]
            (om/build li-component [i])))))

(om/root my-component
               state
               {:target (. js/document (getElementById "troublems"))
                :shared (:shared @state)})

