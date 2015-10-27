(ns monr.test
  (:use clojure.test
        monr
        [clojure.tools.logging]))

(defn ! [n] (reduce *' (range 1 (inc n))))

(deftest info-logging

  (testing "should log stats"

           (dotimes [_ 1000000] 
             (rate "42 factorial") (! 42))))

