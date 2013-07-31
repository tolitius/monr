(ns monr.group
  (:use [monr.util]
        [clojure.tools.logging]))

(defn publish-group [group]
  (info "
/-------------------------------------------------------------------------\\
|        Name        |           Rate        |            Total           |
|-------------------------------------------------------------------------|"
    (apply str 
           (for [[id {:keys [rate current]}] (dissoc group :publishing)]
             (format "\n| %17s  | %,13d ops/sec | %,26d |" id (long rate) current)))
"\n\\-------------------------------------------------------------------------/"))

(defn add-to-group [group {:keys [id] :as rate}]
  (swap! group assoc id (dissoc rate :id))
  (if-not (:publishing @group) (do                  ;; TODO: make group publishing rate configurable
    (every 5 #(publish-group @group))               ;; TODO: have a reference to this future, so it can be canceled
    (swap! group assoc :publishing true))))
    
(def link (partial add-to-group (atom {})))         ;; TODO: embed in monitor to enable different groups 
