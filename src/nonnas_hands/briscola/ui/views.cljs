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

(defn render-player [{:keys [hand face name] :or {face default-face}} pos]
  {:impi/key             name
   :pixi.object/type     :pixi.object.type/container
   :pixi.object/position (let [[x y] pos]
                           [x (- y 30)])
   :pixi.container/children
   (let [cards (sort-cards hand)]
     (concat
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
                       {:impi/key             (str "game/hand-" i "-points")
                        :pixi.object/type     :pixi.object.type/text
                        :pixi.object/position [(* card-w -0.48) (* card-h -0.5)]
                        :pixi.text/text       (str (get rules/points (:rank c) ""))
                        :pixi.text/style      {:pixi.text.style/align       "right"
                                               :pixi.text.style/fill        0x1a77ba
                                               :pixi.text.style/font-weight "normal"
                                               :pixi.text.style/font-family "Arial"
                                               :pixi.text.style/font-size   28}}))]})
       cards)
      [{:impi/key             (str "game/hand-" name "-faces")
        :pixi.object/type     :pixi.object.type/text
        :pixi.object/position [(* card-w -0.7) (* card-h 0.55)]
        :pixi.text/text       face
        :pixi.text/style      {:pixi.text.style/align       "right"
                               :pixi.text.style/fill        0x1a77ba
                               :pixi.text.style/font-weight "normal"
                               :pixi.text.style/font-family "Arial"
                               :pixi.text.style/font-size   62}}
       {:impi/key             (str "game/hand-" name "-names")
        :pixi.object/type     :pixi.object.type/text
        :pixi.object/position [(* card-w -0.1) (* card-h 0.65)]
        :pixi.text/text       name
        :pixi.text/style      {:pixi.text.style/align       "right"
                               :pixi.text.style/fill        0x1a77ba
                               :pixi.text.style/font-weight "normal"
                               :pixi.text.style/font-family "Arial"
                               :pixi.text.style/font-size   28}}
       ]))})

(defn point-on-circle [radius angle]
  [(* radius (Math/sin (* angle PIXI/DEG_TO_RAD)))
   (* radius (Math/cos (* angle PIXI/DEG_TO_RAD)))])

(defn render-players [players radius]
  {:impi/key         :game/hands
   :pixi.object/type :pixi.object.type/container
   :pixi.container/children
   (map-indexed (fn [i player]
                  (let [space (/ 360 (count players))]
                    (render-player player (point-on-circle radius (* space i)))))
                players)})

(defn render-deck [{:keys [deck briscola]} pos]
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
