(ns monr.util
  (:use [clojure.tools.logging]
        [clojure.pprint :only (cl-format)])
  (:import [java.util.concurrent Executors TimeUnit]))

(defn every
  ([interval fun] 
   (every interval fun TimeUnit/SECONDS))
  ([interval fun time-unit] 
   (let [f #(try (fun) (catch Exception e (error (.printStackTrace e System/out))))]
    (.scheduleAtFixedRate (Executors/newScheduledThreadPool 1) 
      f 0 interval time-unit))))

(defn center [s width]
  (cl-format nil "~V<~;~A~;~>" width s))

