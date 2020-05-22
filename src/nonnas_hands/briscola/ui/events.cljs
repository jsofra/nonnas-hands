(ns nonnas-hands.briscola.ui.events)

(defmulti handle-event :key)

(defmethod handle-event :hand/hover-card
  [{{:keys [card]} :args :as e}]
  [{:type      :update
    :key       :hand/select-card-update
    :update-fn #(update % :ui-state assoc :hover-card card)}])

(defn current-player-pulse [duration]
  {:type       :animation
   :key        :player/pulser
   :steps      [{:progress 0
                 :duration (* duration 0.5)
                 :update-gen
                 (fn [t]
                   {:type :update
                    :key  :player/pulse-update
                    :update-fn
                    (fn [state]
                      (assoc-in state [:ui-state :current-player-pulse] t))})}
                {:progress 0
                 :duration (* duration 0.5)
                 :update-gen
                 (fn [t]
                   {:type :update
                    :key  :player/pulse-shrink-update
                    :update-fn
                    (fn [state]
                      (assoc-in state [:ui-state :current-player-pulse] (Math/abs (dec t))))})}]
   :post-steps (fn [{:game/keys [paused?]}]
                 (when (not paused?)
                   [{:type :event
                     :key  :player/pulse
                     :args {:duration duration}}]))})

(defmethod handle-event :player/pulse
  [{{:keys [duration]} :args}]
  [(current-player-pulse duration)])
