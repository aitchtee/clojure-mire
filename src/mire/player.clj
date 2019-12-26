(ns mire.player)

(def ^:dynamic *current-room*)
(def ^:dynamic *current-emoji*)
(def ^:dynamic *inventory*)
(def ^:dynamic *emoji-available*)
(def ^:dynamic *player-name*)
(def ^:dynamic *player-id*)
(def ^:dynamic *player-channel* 0)


(def prompt "> ")
(def player-streams (ref {}))

(defn carrying?
  [thing
;;    player-inventory
   ]

;;   (some #{(keyword thing)} @player-inventory)
  (some #{(keyword thing)} @*inventory*)
  )

(def connections (ref {}))
(def connected_name_channel (ref {}))