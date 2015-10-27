(ns monr.group
  (:use [monr.util]
        [clojure.tools.logging]))

(def ^:private pubs (atom {}))     ;; TODO: embed this into a monitor to avoid a global ref

(defn stop-pub [id]                ;; TODO: set "publishing" to false for this id
  (if-let [pub (get @pubs id)]
    (future-cancel pub)))

(defn has-rate? [group]
  (let [rates (vals (dissoc group :publishing))]
    (some #(not= (:rate %) 0.0) rates)))

(defn publish-group [group]
  (when (has-rate? group)          ;; TODO: make it configurable (e.g. muting zero rates)
    (info "
/-------------------------------------------------------------------------\\
|        Name        |           Rate        |            Total           |
|-------------------------------------------------------------------------|"
      (apply str 
             (for [[id {:keys [rate current]}] (dissoc group :publishing)]
               (format "\n| %17s  | %,13d ops/sec | %,26d |" id (long rate) current)))
"\n\\-------------------------------------------------------------------------/")))

(defn add-to-group [group {:keys [id interval] :as rate}]
  (swap! group assoc id (dissoc rate :id))
  (if-not (:publishing @group)                      ;; TODO: make group publishing rate configurable
    (let [pub (every interval #(publish-group @group))]
      (swap! pubs assoc id pub)
      (swap! group assoc :publishing true))))
    
(def link (partial add-to-group (atom {})))         ;; TODO: embed in monitor to enable different groups 
