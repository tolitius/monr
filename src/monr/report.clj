(ns monr.report
  (:use [clojure.tools.logging]))

(defn default-report [{:keys [id rate current]}] 
  (info (format "%20s rate: %,12d/sec,  total count: %,13d" id (long rate) current)))

(defn pretty-report [{:keys [id rate current]}]
  (when (> rate 0.0)
    (info "
/-------------------------------------------------------------------------\\
|        Name        |           Rate        |            Total           |
|-------------------------------------------------------------------------|"
(format "\n| %17s  | %,13d ops/sec | %,26d |" id (long rate) current)
"\n\\-------------------------------------------------------------------------/")))

;; TODO sample for a queue publisher i.e.

#_(defn publish [{:keys [id rate current] :as rate}]
  ;; ...
  (send queue rate))

