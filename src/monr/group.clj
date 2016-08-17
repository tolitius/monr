(ns monr.group
  (:use [monr.util]
        [clojure.tools.logging]))

;; TODO needs to be refactored to remove all the unmanaged global state

(def ^:private group (atom {}))    ;; TODO: embed this into a monitor to avoid a global ref

(defn stop-pub [id]                ;; TODO: set "publishing" to false for this id
  (when (and (:publishing @group)
             (= (count @group) 2)       ;; a single id is left 
             (get @group id))
    (.shutdownNow (:publishing @group))
    (reset! group {}))
  (swap! group dissoc id))

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
      (swap! group assoc :publishing pub)))) 

(def link (partial add-to-group group))             ;; TODO: embed in monitor to enable different groups 
