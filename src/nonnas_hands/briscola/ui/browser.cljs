(ns nonnas-hands.briscola.ui.browser
  (:require [impi.core :as impi]
            [nonnas-hands.briscola.ui.impi]
            [nonnas-hands.briscola.ui.views :as views]))

(def doc-element (.getElementById js/document "app"))

(defn render-state [state]
  (impi/mount :game state doc-element))

(defn init-stage []
  (let [d     1200
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
                   (views/render-hands [[{:rank :re :suit :spade}
                                         {:rank :tre :suit :spade}
                                         {:rank :uno :suit :spade}]
                                        [{:rank :re :suit :spade}
                                         {:rank :tre :suit :spade}
                                         {:rank :uno :suit :spade}]
                                        [{:rank :re :suit :spade}
                                         {:rank :tre :suit :spade}
                                         {:rank :uno :suit :spade}]
                                        [{:rank :re :suit :spade}
                                         {:rank :tre :suit :spade}
                                         {:rank :uno :suit :spade}]]
                                       (/ js/window.innerHeight 3 scale))
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
