(ns nonnas-hands.briscola.rules
  (:require [nonnas-hands.characters :as charaters]))

;; the rank of the cards in Briscola is not according to the face value
(def ranks [:due :quattro :cinque :sei :sette :fante :cavallo :re :tre :uno])
(def rankings (zipmap ranks (map inc (range))))

(def suits #{:spade :coppe :denari :bastoni})

(def deck (set (for [r ranks s suits] {:rank r :suit s})))

(def points {:uno     11
             :tre     10
             :re      4
             :cavallo 3
             :fante   2})

(defn cards-to-remove
  "
  Depending on the number of players some cards must be removed from the deck before play.
  Return a set of cards to be removed.
  "
  [player-count]
  (case player-count
    3 #{{:rank :due :suit :spade}}
    6 #{{:rank :due :suit :spade} {:rank :due :suit :coppe}}
    #{}))

(defn form-teams
  "
  When there are an even number of players they will be grouped into two teams.
  Pairs of players sitting next to each other should be on opposite teams.

  When there are an odd numbers of players they play individually.
  "
  [players]
  (if (even? (count players))
    (let [pairs (partition 2 players)]
      [(mapv first pairs) (mapv second pairs)])
    (mapv vector players)))

(defn deal-init-cards
  "
  The initial deal is 3 cards for each player.
  "
  [{:keys [player-ids deck] :as game-state}]
  (-> game-state
      (update :deck #(drop (* 3 (count player-ids)) %))
      (assoc :players (into {} (map (fn [id hand] [id {:hand (into [] hand)
                                                       :id   id}])
                                    player-ids
                                    (partition 3 deck))))))

(defn draw-briscola
  "
  Draw the briscola (trump card) from the deck.
  "
  [{:keys [deck] :as game-state}]
  (-> game-state
      (update :deck pop)
      (assoc :briscola (peek deck))))

(defn init-game-state
  "
  The game state is a map that will keep track of the cards in the deck and players hands.

  The state will be updated as each player takes a turn.

  Start the game by removing unneeded cards from the deck and giving it shuffle.
  The teams are also formed.
  "
  [deck player-ids]
  (let [cards-to-remove    (cards-to-remove (count player-ids))]
    {:deck              (shuffle (remove cards-to-remove deck))
     :removed-cards     cards-to-remove
     :player-ids        player-ids
     :teams             (form-teams player-ids)
     :players           (zipmap player-ids (repeat {:hand [] :tricks []}))
     :remaining-players player-ids
     :trick-pile        {}
     :current-player    (rand-nth player-ids)}))

(defn assign-player
  "
  Assigns a player for `this` instance of the game.
  "
  [game-state player-id]
  (assoc game-state :this-player player-id))

(defn play-card
  "
  A player plays a card.
  1. Remove the card from the players hand.
  2. Add the card to the trick-pile.
  3. Move the trick forward to the next player.
  "
  [game-state player-id card]
  (-> game-state
      (update-in [:players player-id :hand] (fn [hand] (into [] (remove #{card} hand))))
      (update :trick-pile conj [player-id card])
      (update :remaining-players rest)))
