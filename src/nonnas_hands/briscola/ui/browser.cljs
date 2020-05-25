(ns nonnas-hands.briscola.ui.browser
  (:require [cljs.core.async :as async]
            [impi.core :as impi]
            ["pixi.js" :as PIXI]
            [nonnas-hands.briscola.ui.impi]
            [nonnas-hands.briscola.ui.views :as views]
            [nonnas-hands.briscola.rules :as rules]
            [nonnas-hands.briscola.ui.animate :as animate]
            [nonnas-hands.briscola.ui.events :as events]))

(defonce state (atom nil))
(defonce msg-chan (async/chan))

(defn init-state! []
  (reset!
   state
   (-> rules/deck
       (rules/init-game-state ["James" "Nikki" "Susanna" "Tanya"])
       (rules/assign-player "James")
       rules/draw-briscola
       rules/deal-init-cards
       (as-> s
           #_(rules/play-card s "James" (get-in s [:players "James" :hand 0]))
           #_(rules/play-card s "Nikki" (get-in s [:players "Nikki" :hand 0]))
           #_(rules/play-card s "Susanna" (get-in s [:players "Susanna" :hand 0]))
           #_(rules/play-card s "Tanya" (get-in s [:players "Tanya" :hand 0]))

           )
       ))
  (async/put! msg-chan {:type :event
                        :key  :player/pulse
                        :args {:duration 160}}))

(defn render-state
  [msg-chan state]
  (let [d     1050
        scale (/ js/window.innerHeight d)]
    {:pixi/renderer {:pixi.renderer/size             [js/window.innerWidth js/window.innerHeight]
                     :pixi.renderer/background-color 0xffffff
                     :pixi.renderer/transparent?     false
                     :pixi.renderer/antialias?       true}

     :impi/events-chan msg-chan

     :pixi/stage {:impi/key             :stage
                  :pixi.object/type     :pixi.object.type/container
                  :pixi.object/scale    [scale scale]
                  :pixi.object/position [(/ js/window.innerWidth 2)
                                         (/ js/window.innerHeight 2)]
                  :pixi.container/children
                  [(views/background)
                   (views/render-deck state
                                      (min (/ js/window.innerWidth 2.8 scale) 300)
                                      (/ js/window.innerHeight 2.8 scale))
                   (views/render-players state
                                         (min (/ js/window.innerWidth 2.8 scale) 420)
                                         (/ js/window.innerHeight 2.8 scale))]}}))

(defn compact-events [events-map {:keys [parent] :as event}]
  (update events-map
          (select-keys event [:type :key])
          #(into (set %1) (if %2 [%2] []))
          (or (:key parent) :game/UI)))

(defonce events-map (atom {}))

(defn take-all! [chan msg-keys]
  (loop [elements (zipmap msg-keys (repeat []))]
    (if-let [element (async/poll! chan)]
      (do
        (swap! events-map compact-events element)
        (if (contains? (set msg-keys) (:type element))
          (recur (update elements (:type element) conj element))))
      elements)))

(defn put-all! [chan msgs parent]
  (doseq [msg msgs]
    (async/put! chan (assoc msg :parent (dissoc parent :parent)))))

(def app-element (.getElementById js/document "app"))

(defn mount-game! []
  (when @state (impi/mount :game (render-state msg-chan @state) app-element)))

(defn game-handler [mount-fn delta-time]

  ;; process all the new events
  ;; may generate updates/animations/events

  (let [{events     :event
         updates    :update
         animations :animation} (take-all! msg-chan [:event
                                                     :update
                                                     :animation])]

    (doseq [e events]
      (put-all! msg-chan (events/handle-event e) e))

    ;; process any animations
    (doseq [a animations]
      (put-all! msg-chan (animate/apply-animation delta-time a) a)

      (when (and (:post-steps a) (not (:steps a)))
        (put-all! msg-chan ((:post-steps a) @state) a)))

    ;; process all the updates
    (when (seq updates)
      (let [[old new] (swap-vals! state (apply comp (map :update-fn updates)))]
        (doseq [update updates]
          (when (:reaction update)
            (put-all! msg-chan ((:reaction update) old new) update))))))

  (mount-fn))

(defonce ticker (atom nil))

(defn start-ticker! []
  (reset! ticker
          (doto (PIXI/Ticker.)
            (.stop)
            (.add (partial game-handler mount-game!))
            (.start))))

(defn destory-ticker! []
  (if-let [ticker' @ticker]
    (do (doto ticker'
          (.stop)
          (.destroy))
        (reset! ticker nil))))

(defn start []
  (init-state!)
  (start-ticker!))

(defn pause []
  (if-let [ticker @ticker]
    (.stop ticker)))

(defn continue []
  (if-let [ticker @ticker]
    (.start ticker)))

(defn stop []
  (destory-ticker!))

(defn reset []
  (stop)
  (start))

(defn ^:export init []
  (js/console.log "init")
  (reset))

#_(defn ^:dev/after-load after []
    (js/console.log "start")
    (reset))

#_(defn ^:dev/before-load stop []
    (js/console.log "stop")
    (impi/unmount :game))
