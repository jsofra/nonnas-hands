(ns nonnas-hands.signalling
  (:require ["simple-peer" :as Peer]
            ["firebase/app" :as firebase]
            ["firebase/analytics"]
            ["firebase/firestore"]
            [cljs.tools.reader.edn :as edn]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]))

(def firebase-config #js {:apiKey            "AIzaSyArLucPHnKX2QU4j-S92lyh4d20yt0tnMY"
                          :authDomain        "nonnas-hands-e73e9.firebaseapp.com"
                          :databaseURL       "https://nonnas-hands-e73e9.firebaseio.com"
                          :projectId         "nonnas-hands-e73e9"
                          :storageBucket     "nonnas-hands-e73e9.appspot.com"
                          :messagingSenderId "56659697076"
                          :appId             "1:56659697076:web:c6a159b3e0d5fb1c57a6e1"
                          :measurementId     "G-7KN5ZC5M50"})

(defn init-db []
  (firebase/initializeApp firebase-config)
  (firebase/analytics))

(def room-codes
  (let [A-Z "ABCDEFGHIJKLMNOPQRSTUVWXYZ"]
    (for [a A-Z
          b A-Z
          c A-Z
          d A-Z]
      (str a b c d))))

(defn create-room [db room-name player-id]
  (let [set-room  (fn [ref]
                    (.set ref
                          (clj->js {:name       room-name
                                    :owner      player-id
                                    :created-at (.now firebase/firestore.Timestamp)}))
                    ref)]
    (go
      (loop [codes (shuffle room-codes)]
        (let [ref  (.doc (.collection db "rooms") (first codes))
              room (.data (<p! (.get ref)))]
          (if room
            (let [since      (- (.now js/Date) 1800000)
                  created-at (.toMillis (goog.object/get room "created-at"))]
              (if (< created-at since)
                (.-id (set-room ref))
                (recur (rest codes))))
            (.-id (set-room ref))))))))

(defn get-players-ref [db room-id]
  (.collection db (str "rooms/" room-id "/players")))

(defn create-player [db room-id player-name]
  (go
    (let [player-ref (<p! (.add (get-players-ref db room-id) (clj->js {:name player-name})))]
      (.setItem js/window.localStorage (str "nonnas-hands/player-id-" room-id) (.-id player-ref))
      (.setItem js/window.localStorage "nonnas-hands/player-name" player-name)
      player-ref)))

(defn get-players [db room-id]
  (go
    (let [players-ref (<p! (.get (get-players-ref db room-id)))]
      (mapv #(.data %) (.-docs players-ref)))))


(defn get-local-player-name []
  (.getItem js/window.localStorage "nonnas-hands/player-name"))


(defn ^:export init []
  (init-db)
  (let [player-name (.getItem js/window.localStorage "nonnas-hands/player-name")
        room-id     (.get (js/URLSearchParams. js/window.location.search) "room-id")]
    (set! (.-value (.querySelector js/document "#player-name-input")) player-name)
    (if room-id
      (do
        (set! (.-hidden (.querySelector js/document "#create-room-div")) true)
        (set! (.-value (.querySelector js/document "#join-room-input")) room-id))
      (do
        (set! (.-hidden (.querySelector js/document "#join-room-div")) true)))))


(def peer (Peer. #js {:initiator true}))
