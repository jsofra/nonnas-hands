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

(defonce state (atom {:peers     {}
                      :player-id nil}))

(def storage (if ^boolean js/goog.DEBUG
               js/window.sessionStorage
               js/window.localStorage))

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

(defn generate-room [db]
  (let [set-room  (fn [ref]
                    (.set ref (clj->js {:created-at (.now firebase/firestore.Timestamp)}))
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
      (.setItem storage (str "nonnas-hands/player-id-" room-id) (.-id player-ref))
      (.setItem storage "nonnas-hands/player-name" player-name)
      player-ref)))

(defn get-players [db room-id]
  (go
    (let [players-ref (<p! (.get (get-players-ref db room-id)))]
      (.-docs players-ref))))

(defn populate-players-list [players]
  (let [players-list (.querySelector js/document "#players-list")]
    (set! (.-innerHTML players-list) "")
    (doseq [player players]
      (let [player-data (.data player)]
        (.appendChild players-list
                      (doto (.createElement js/document "li")
                        (goog.object/set "innerText"
                                         (goog.object/get player-data "name"))
                        (goog.object/set "className" "list-group-item")))))))

(defn set-room-url [room-id]
  (set! (.-innerText (.querySelector js/document "#room-url"))
        (str js/window.location.protocol
             "//"
             js/window.location.host
             js/window.location.pathname
             "?room-id=" room-id)))

(defn create-pairings [players]
  (->> (for [a players
             b players
             :when (not= a b)]
         [a b])
       (group-by set)
       vals
       (mapv first)))

(defn peers-config [this-id pairings]
  (apply merge
         (for [[a b] pairings
               :let [peer-id (if (= a this-id) b a)]
               :when (or (= a this-id) (= b this-id))]
           {peer-id {:initiator (= a this-id)}})))

(defn get-peer-ref [db room-id player-id peer-id]
  (-> db
      (get-players-ref room-id)
      (.doc player-id)
      (.collection "peers")
      (.doc peer-id)))

(defn add-peer-signalling [db room-id player-id peer-id data]
  (-> (get-peer-ref db room-id player-id peer-id)
      (.set #js {"signalling" data})))

(defn create-peer [state db room-id player-id [peer-id peer-config]]
  (let [peer (Peer. (clj->js peer-config))]

    (-> (get-peer-ref db room-id peer-id player-id)
        (.onSnapshot
         (fn [peer-snapshot]
           (let [signal-data (.get peer-snapshot "signalling")]
             (when signal-data
               (js/console.log "SIG" signal-data)
               (.signal peer signal-data))))))

    (doto peer
      (.on "signal"
           (fn [data]
             (add-peer-signalling db room-id player-id peer-id (clj->js data))))
      (.on "connect"
           (fn []
             (.send peer (str "Connecting with " player-id))
             (swap! state assoc-in [:peers peer-id :connected] true)))
      (.on "data"
           (fn [data]
             (js/console.log (str "Got a message: " data)))))

    (swap! state assoc-in [:peers peer-id :peer] peer)))

(defn get-peers-config [state db room-id]
  (go
    (let [{:keys [player-id]} @state]
      (->> (<! (get-players db room-id))
           (map #(.-id %))
           create-pairings
           (peers-config player-id)))))

(defn create-peers [state db peers-config]
  (let [{:keys [room-id player-id peers]} @state]
    (doseq [peer-config peers-config]
      (create-peer state db room-id player-id peer-config))))

(defn wait-for-start [state db room-id]
  (.onSnapshot (.doc (.collection db "rooms") room-id)
               (fn [snapshot]
                 (when (.get snapshot "game-started")
                   (go
                     (create-peers state db (<! (get-peers-config state db room-id))))))))

(defn init-wait [state db room-id wait-element]
  (let [player-name (.-value (.querySelector js/document "#player-name-input"))
        players-ref (get-players-ref db room-id)]
    (set-room-url room-id)
    (.onSnapshot players-ref
                 (fn [players-snapshot]
                   (populate-players-list (.-docs players-snapshot))))
    (go (swap! state
               assoc
               :room-id room-id
               :player-id (.-id (<! (create-player db room-id player-name)))))
    (set! (.-hidden (.querySelector js/document "#init")) true)
    (set! (.-hidden (.querySelector js/document "#wait")) false)
    (set! (.-hidden (.querySelector js/document wait-element)) false)

    (wait-for-start state db room-id)))

(defn ^:export create-room []
  (go
    (let [db      (firebase/firestore)
          room-id (<! (generate-room db))]
      (init-wait state db room-id "#wait-create"))))

(defn ^:export join-room []
  (go
    (let [db      (firebase/firestore)
          room-id (.get (js/URLSearchParams. js/window.location.search) "room-id")]
      (init-wait state db room-id "#wait-join"))))

(defn ^:export copy-url []
  (.writeText js/navigator.clipboard
              (.-innerText (.querySelector js/document "#room-url"))))

(defn ^:export init []
  (init-db)
  (let [player-name (.getItem storage "nonnas-hands/player-name")
        room-id     (.get (js/URLSearchParams. js/window.location.search) "room-id")]
    (set! (.-value (.querySelector js/document "#player-name-input")) player-name)
    (if room-id
      (do
        (set! (.-hidden (.querySelector js/document "#init-join")) false)
        (set! (.-value (.querySelector js/document "#join-room-input")) room-id))
      (set! (.-hidden (.querySelector js/document "#init-create")) false))))


(defn start-game [db room-id]
  (-> (.collection db "rooms")
      (.doc room-id)
      (.update #js {:game-started true})))

(def peer (Peer. #js {:initiator true}))
