(ns monr.measure
  (:require [clojure.repl :refer [set-break-handler!]])
  (:use monr
        [monr report]
        [clojure.tools.logging]))

(defn seqs-to-map [seqs]
  (apply hash-map 
         (reduce concat seqs)))

(defmacro measure [id & opts]
  (let [;; params# (seqs-to-map [[:id id :group false :publish pretty-report] opts])
        mon# (rate :id id :group false :publish pretty-report)
        rate-on#  (:inc-count mon#)]
     (set-break-handler! (fn [s#] (stop mon#)))          ;; stop the monitor on "Ctrl + C"
    `(~(intern *ns* 
               (symbol (gensym id)) 
               #(rate-on#)))))
