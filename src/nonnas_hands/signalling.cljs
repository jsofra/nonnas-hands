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

(defn create-room [db room-name player-id]
  (go
    (let [room-ref (<p! (.add (.collection db "rooms")
                              (clj->js {:name room-name
                                        :owner player-id})))]
      room-ref)))

(defn get-players-ref [db room-id]
  (.collection db (str "rooms/" room-id "/players")))

(defn create-player [db room-id player-name]
  (go
    (let [player-ref (<p! (.add (get-players-ref db room-id) (clj->js {:name player-name})))]
      (.setItem js/window.localStorage "nonnas-hands/player-id" (.-id player-ref))
      player-ref)))

(defn get-players [db room-id]
  (go
    (let [players-ref (<p! (.get (get-players-ref db room-id)))]
      (mapv #(.data %) (.-docs players-ref)))))

(def peer (Peer. #js {:initiator true}))
