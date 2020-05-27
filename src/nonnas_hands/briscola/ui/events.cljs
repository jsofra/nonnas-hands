(ns nonnas-hands.briscola.ui.events)

(defmulti handle-event :key)

(defmethod handle-event :hand/hover-card
  [{{:keys [card player]} :args :as e}]
  [{:type      :update
    :key       :hand/hover-card-update
    :update-fn #(update-in % [:players player] vary-meta assoc :hover-card card)}])

(defmethod handle-event :hand/select-card
  [{{:keys [card]} :args :as e}]
  [{:type      :update
    :key       :hand/select-card-update
    :update-fn #(update % :players vary-meta assoc :selected card)}])

(defn current-player-pulse [duration]
  {:type       :animation
   :key        :player/pulser
   :steps      [{:progress 0
                 :duration (* duration 0.5)
                 :update-gen
                 (fn [t]
                   {:type :update
                    :key  :player/pulse-update
                    :update-fn #(update % :players vary-meta assoc :pulse t)})}
                {:progress 0
                 :duration (* duration 0.5)
                 :update-gen
                 (fn [t]
                   {:type :update
                    :key  :player/pulse-shrink-update
                    :update-fn #(update % :players vary-meta assoc :pulse (Math/abs (dec t)))})}]
   :post-steps (fn [{:game/keys [paused?]}]
                 (when (not paused?)
                   [{:type :event
                     :key  :player/pulse
                     :args {:duration duration}}]))})

(defmethod handle-event :player/pulse
  [{{:keys [duration]} :args}]
  [(current-player-pulse duration)])
