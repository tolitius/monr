(ns monr
  (:use [monr util group report]
        [clojure.tools.logging]))

(def ^:private monitors (atom {}))
(def ^:private muted (atom #{}))

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
    {:current-rate #(update-stats (partial calc-rate interval current previous) stats)
     :latest-rate stats
     :count current
     :inc-count #(swap! current inc)
     :update-current latest-count}))

(defn read-rate [{:keys [id latest-rate]}] 
  (assoc (deref latest-rate)
         :id id))

(defn rate [& {:keys [interval           ;; how often the rate gets published    :default 5 seconds
                      id                 ;; id/name of this rate                 :default (gensym "id:")
                      publish            ;; publisher function                   :default "default-report"
                      update             ;; function to update the rate          :default nil
                      group]             ;; whether to add this rate to a group  :default true
               :or   {id (gensym "id:")
                      interval 5                 ;; time unit is seconds (assumed for now)
                      publish default-report
                      group true}}]
  (let [{:keys [current-rate 
                update-current] :as meter} (rate-meter interval update)
        mon (if group (every interval #(do
                                         (update-current) 
                                         (link (assoc (current-rate) :id id))))
                      (every interval #(do
                                         (update-current)
                                         (publish (assoc (current-rate) :id id)))))]
    (swap! monitors assoc id mon)
    (merge {:mon mon :id id} meter)))

(defn mute [id]
  (swap! muted conj (keyword id)))

(defn stop [{:keys [mon fun]}]
  (if mon (future-cancel mon))
  (if fun (future-cancel fun)))

(defn stop-all []
  (for [[id mon] @monitors] (do
    (stop {:mon mon})
    (stop-pub id))))

(comment
  """
    FEATURES:

      1. Return an :inc-count function
      2. Take a total count update function
      3. Inline (measure 'name') into code
      4. Being able to read rates on demand
      5. Bench a function (in REPL)

    TODO:
     
      1. Have a stop all including publish groups and rates created by macros
      2. Mute publishers (useful if the rates are only read on demand)
  """)

