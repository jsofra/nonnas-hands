(ns nonnas-hands.briscola.ui.browser
  (:require [impi.core :as impi]
            [nonnas-hands.briscola.ui.impi]
            [nonnas-hands.briscola.ui.views :as views]
            [nonnas-hands.briscola.rules :as rules]))

(def doc-element (.getElementById js/document "app"))

(defn render-state [state]
  (impi/mount :game state doc-element))

(def init-state (-> rules/deck
                    (rules/init-game-state ["James" "Nikki" "Susanna" "Tanya"])
                    rules/draw-briscola
                    rules/deal-init-cards))

(defn init-stage []
  (let [d     1100
        scale (/ js/window.innerHeight d)]
    {:pixi/renderer {:pixi.renderer/size             [js/window.innerWidth js/window.innerHeight]
                     :pixi.renderer/background-color 0xffffff
                     :pixi.renderer/transparent?     true
                     :pixi.renderer/antialias?       true}

     :pixi/stage {:impi/key             :stage
                  :pixi.object/type     :pixi.object.type/container
                  :pixi.object/scale    [scale scale]
                  :pixi.object/position [(/ js/window.innerWidth 2)
                                         (/ js/window.innerHeight 2)]
                  :pixi.container/children
                  [(views/background)
                   (views/render-players (vals (:players init-state))
                                         (/ js/window.innerHeight 2.8 scale))
                   (views/render-deck init-state
                                      [(/ js/window.innerHeight 3 scale)
                                       (/ js/window.innerHeight -4 scale)])
                   ]}}))


(defn ^:export init []
  (js/console.log "init")
  (render-state (init-stage)))

(defn ^:dev/after-load start []
  (js/console.log "start")
  (render-state (init-stage)))

(defn ^:dev/before-load stop []
  (js/console.log "stop")
  (impi/unmount :game))
