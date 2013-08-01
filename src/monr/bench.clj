(ns monr.bench
  (:require [clojure.repl :refer [set-break-handler!]])
  (:use monr
        [monr report]
        [clojure.tools.logging]))

(defn tight-inc-loop [f {:keys [inc-count]}]          ;; TODO: Should be stoppable, yet stay min overhead
  (loop [] 
    (f) 
    (inc-count) 
    (recur)))

(defn gentle-inc-loop [f {:keys [mon inc-count]}] 
  (loop [] 
    (if-not (.isCancelled mon) 
      (do 
        (f) 
        (inc-count) 
        (recur)))))

(defmacro bench [body]
  `(let [r# (rate :group false 
                  :publish pretty-report)]
     (set-break-handler! (fn [s#] (stop r#)))  ;; stop the monitor on "Ctrl + C" in REPL
     (tight-inc-loop #(~@body) r#)))
