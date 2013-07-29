(ns monr
  (:use [monr util group]
        [clojure.tools.logging]))

(defn calc-rate [interval current previous] 
  ;; TODO: consider time unit. for now assuming seconds
  (let [rate (double (/ (- @current @previous) interval))]
    (reset! previous @current)
    {:rate rate :current @current}))

(defn- update-stats [calc stats]
  (let [rate (calc)]
    (reset! stats rate)))

(defn- rate-meter [interval update]
  (let [current (atom 0)
        previous (atom 0)
        stats (atom {})
        latest-count (if update
                       #(reset! current (update)) 
                       #())]
    {:rate #(update-stats (partial calc-rate interval current previous) stats)
     :latest-rate stats
     :inc-count #(swap! current inc)
     :update-current latest-count}))

(defn default-report [{:keys [id rate current]}] 
  (info (format "%20s rate: %,12d/sec,  total count: %,13d" id (long rate) current)))

(defn cancel [m]
  (.cancel (:monitor m) true))

(defn rate [m] 
  (deref (:latest-rate m)))

(defn monitor [& {:keys [interval           ;; how often the rate gets published    :default 5 seconds
                         id                 ;; id/name of this rate                 :default (gensym "id:")
                         publish            ;; publisher function                   :default "default-report"
                         update             ;; function to update the rate          :default nil
                         group]             ;; whether to add this rate to a group  :default true
                  :or   {id (gensym "id:")
                         interval 5                 ;; time unit is seconds (assumed for now)
                         publish default-report
                         group true}}]
  (let [{:keys [rate 
                update-current] :as meter} (rate-meter interval update)
        mon (if group (every interval #(do
                                         (update-current) 
                                         (link (assoc (rate) :id id))))
                      (every interval #(do
                                         (update-current)
                                         (publish (assoc (rate) :id id)))))]
    (merge {:monitor mon} meter)))

