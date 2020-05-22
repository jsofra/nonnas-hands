(ns nonnas-hands.briscola.ui.views
  (:require [nonnas-hands.briscola.rules :as rules]
            ["pixi.js" :as PIXI]
            [goog.color :as color]))

(defn background []
  {:impi/key             :game/tabletop
   :pixi.object/type     :pixi.object.type/sprite
   :pixi.object/position [0 0]
   :pixi.object/scale    [0.6 0.6]
   :pixi.sprite/anchor   [0.5 0.5]
   :pixi.sprite/texture  {:pixi.texture/source "img/tabletopsmall.jpg"}})

(def card-defaults
  (merge #:pixi.sprite {:tint     0xFFFFFF
                        :anchor   [0.5 0.5]}
         #:pixi.object {:position [0 0]
                        :rotation 0}))

(defn render-card
  ([card]
   (render-card card [0 0]))
  ([{:keys [rank suit flipped]
     :or   {flipped 0}
     :as   card}
    position]
   (let [card-name (keyword (str (name rank) "-" (name suit)))
         flipped   (if (zero? (mod (+ flipped 90) 180)) (+ flipped 0.5) flipped)
         revealed  (odd? (Math/ceil (/ (+ flipped 90) 180)))]
     {:impi/key             card-name
      :card/flipped         flipped
      :card/revealed        revealed
      :pixi.object/position position
      :pixi.object/type     :pixi.object.type/container
      :pixi.container/children
      [{:impi/key           (str card-name "-shadow")
        :pixi.object/type   :pixi.object.type/sprite
        :pixi.object/alpha  0.5
        :pixi.sprite/anchor [0.5 0.5]
        :pixi.sprite/texture
        {:pixi.texture/source "img/dropshadow.png"}}
       (merge
        card-defaults
        {:impi/key         (str card-name "-card")
         :pixi.object/type :pixi.object.type/sprite
         :pixi.sprite/texture
         {:pixi.texture/source (if revealed
                                 (str "img/" (name card-name) ".png")
                                 (str "img/back.png"))}})]})))

(defn sort-cards [cards]
  (sort-by (juxt :suit (comp rules/rankings :rank)) cards))

(def card-w 122)
(def card-h 200)


(def default-face "🙂")
(def grimace-face "😬")
(def lookup-face "🙄")
(def tongue-face "😋")
(def side-face "🤔")
(def shrug-face "🤨")

(defn current-player-tint [current-player-pulse]
  (let [green (color/hexToRgb "#00ff26")
        white (color/hexToRgb "#ffffff")]
    (->
     (apply color/rgbToHex (color/blend green white current-player-pulse))
     (clojure.string/replace #"#" "0x")
     js/Number.
     (js/parseInt 10))))

(defn render-player
  [{:keys [hand face id] :or {face default-face} :as player}
   hover-card
   this-player?
   current-player?
   current-player-pulse
   pos
   trick-card
   trick-pos]
  {:impi/key         (str "player-" id "-container")
   :pixi.object/type :pixi.object.type/container
   :pixi.container/children
   (concat
    (when trick-card
      [{:impi/key             (str "player-" id "-trick")
        :pixi.object/type     :pixi.object.type/container
        :pixi.object/position trick-pos
        :pixi.container/children
        [(render-card trick-card)]}])
    [{:impi/key                          (str "player-" id)
      :pixi.object/type                  :pixi.object.type/container
      :pixi.object/position              (let [[x y] pos]
                                           [x (- y 30)])
      :pixi.event/mouse-out              [:hand/hover-card {:card nil}]
      :pixi.object/interactive?          true
      :pixi.container/sortable-children? true
      :pixi.container/children
      (let [cards (sort-cards hand)]
        (concat
         (map-indexed
          (fn [i card]
            {:impi/key             (str id "-card-" i)
             :pixi.object/type     :pixi.object.type/container
             :pixi.object/rotation (let [n     (count cards)
                                         angle (* 25 (/ (dec n) 5))
                                         from  (* -1 angle)
                                         to    angle]
                                     (if (= n 1)
                                       0
                                       (* (+ from (* (* (/ (Math/abs (- from to)) (dec n))) i))
                                          PIXI/DEG_TO_RAD)))
             :pixi.object/position [(let [space 50]
                                      (- (* i space) (* (dec (count cards)) space 0.5)))
                                    (* card-h 0.5)]
             :pixi.object/z-index  (if (= card hover-card) 100 0)
             :pixi.container/children
             [(-> (render-card (assoc card :flipped (if this-player? 0 180)))
                  (assoc :pixi.object/pivot [0 (* card-h 0.5)])
                  (cond-> this-player?
                    (assoc :pixi.event/mouse-over [:hand/hover-card {:card card}]
                           :pixi.object/interactive? true))
                  (update :pixi.container/children
                          conj
                          {:impi/key             (str "game/hand-" i "-points")
                           :pixi.object/type     :pixi.object.type/text
                           :pixi.object/position [(* card-w -0.48) (* card-h -0.5)]
                           :pixi.text/text       (str (get rules/points (:rank card) ""))
                           :pixi.text/style      {:pixi.text.style/align       "right"
                                                  :pixi.text.style/fill        0x1a77ba
                                                  :pixi.text.style/font-weight "normal"
                                                  :pixi.text.style/font-family "Arial"
                                                  :pixi.text.style/font-size   28}}))]})
          cards)
         [{:impi/key             (str "game/hand-" id "-names")
           :pixi.object/type     :pixi.object.type/text
           :pixi.object/position [(* card-w -0.2) (* card-h 0.55)]
           :pixi.object/z-index  200
           :pixi.text/text       id
           :pixi.text/style      {:pixi.text.style/align       "right"
                                  :pixi.text.style/fill        0x1a77ba
                                  :pixi.text.style/font-weight "normal"
                                  :pixi.text.style/font-family "Brush Script MT"
                                  :pixi.text.style/font-size   40}}
          {:impi/key             (str "game/hand-" id "-faces")
           :pixi.object/type     :pixi.object.type/text
           :pixi.object/position [(* card-w -0.8) (* card-h 0.45)]
           :pixi.object/z-index  200
           :pixi.text/text       face
           :pixi.text/style      {:pixi.text.style/align       "right"
                                  :pixi.text.style/font-weight "normal"
                                  :pixi.text.style/font-family "Arial"
                                  :pixi.text.style/font-size   62}
           :pixi.object/tint     (if current-player?
                                   (current-player-tint current-player-pulse)
                                   0xffffff)}]))}])})

(defn point-on-circle [radius angle]
  [(* radius (Math/sin (* angle PIXI/DEG_TO_RAD)))
   (* radius (Math/cos (* angle PIXI/DEG_TO_RAD)))])

(defn render-players
  [{{:keys [players this-player current-player trick-pile]}         :game-state
    {{:keys [hover-card pulse]} :players} :ui-state}
   radius]
  {:impi/key         :game/hands
   :pixi.object/type :pixi.object.type/container
   :pixi.container/children
   (map-indexed (fn [i [player-id player]]
                  (let [spacing (/ 360 (count players))]
                    (render-player player
                                   hover-card
                                   (= player-id this-player)
                                   (= player-id current-player)
                                   pulse
                                   (point-on-circle radius (* spacing i))
                                   (get trick-pile player-id)
                                   (point-on-circle (* radius 0.25) (* spacing i)))))
                players)})

(defn render-deck
  [{{:keys [deck briscola]} :game-state} pos]
  {:impi/key         :game/deck
   :pixi.object/type :pixi.object.type/container
   :pixi.container/children
   (concat
    [(assoc (render-card briscola
                         (let [[x y]  pos]
                           [(- x 100) y]))
            :pixi.object/rotation (* 270 PIXI/DEG_TO_RAD))]
    (map-indexed
     (fn [i card]
       (render-card (assoc card :flipped 180)
                    (let [[x y]  pos
                          offset (* 1 i)]
                      [(+ x offset) (- y offset)])))
     deck))})
