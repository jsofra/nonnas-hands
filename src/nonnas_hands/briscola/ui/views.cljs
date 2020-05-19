(ns nonnas-hands.briscola.ui.views
  (:require [nonnas-hands.briscola.rules :as rules]
            ["pixi.js" :as PIXI]))

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
        :pixi.object/alpha  0.3
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

(defn render-hand [hand name pos]
  {:impi/key             name
   :pixi.object/type     :pixi.object.type/container
   :pixi.object/position pos
   :pixi.container/children
   (let [cards (sort-cards hand)]
     (map-indexed
      (fn [i c]
        {:impi/key             (str name "-card-" i)
         :pixi.object/type     :pixi.object.type/container
         :pixi.object/rotation (let [n     (count cards)
                                     angle (* 25 (/ (dec n) 5))
                                     from  (* -1 angle)
                                     to    angle]
                                 (if (= n 1)
                                   0
                                   (* (+ from (* (* (/ (Math/abs (- from to)) (dec n))) i))
                                      PIXI/DEG_TO_RAD)))
         :pixi.object/position [(let [space (/ 140 (count cards))]
                                  (- (* i space) space))
                                (* 0.5 card-h)]
         :pixi.container/children
         [(-> (render-card c)
              (assoc :pixi.object/pivot [0 (* card-h 0.5)])
              (update :pixi.container/children
                      conj
                      {:impi/key             (str "game/hand-" i "-text")
                       :pixi.object/type     :pixi.object.type/text
                       :pixi.object/position [(* card-w -0.48) (* card-h -0.5)]
                       :pixi.text/text       (str (get rules/points (:rank c) ""))
                       :pixi.text/style
                       {:pixi.text.style/align       "right"
                        :pixi.text.style/fill        0x1a77ba
                        :pixi.text.style/font-weight "normal"
                        :pixi.text.style/font-family "Arial"
                        :pixi.text.style/font-size   28}}))]})
      cards))})

(defn point-on-circle [radius angle]
  [(* radius (Math/sin (* angle PIXI/DEG_TO_RAD)))
   (* radius (Math/cos (* angle PIXI/DEG_TO_RAD)))])

(defn render-hands [hands radius]
  {:impi/key         :game/hands
   :pixi.object/type :pixi.object.type/container
   :pixi.container/children
   (map-indexed (fn [i hand]
                  (let [space (/ 360 (count hands))]
                    (render-hand hand (str "game/hand-" i) (point-on-circle radius (* space i)))))
                hands)})
