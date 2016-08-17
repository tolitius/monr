(ns monr.util
  (:use [clojure.tools.logging]
        [clojure.pprint :only (cl-format)])
  (:import [java.util.concurrent Executors TimeUnit]))

(defn every
  ([interval fun] 
   (every interval fun TimeUnit/SECONDS))
  ([interval fun time-unit] 
   (let [f #(try (fun) (catch Exception e (error (.printStackTrace e System/out))))
         pool (Executors/newScheduledThreadPool 1)]
    (.scheduleAtFixedRate pool f 0 interval time-unit)
    pool)))

(defn center [s width]
  (cl-format nil "~V<~;~A~;~>" width s))

