(ns mire.player)

(def ^:dynamic *current-room*)
(def ^:dynamic *inventory*)
(def ^:dynamic *player-name*)
(def ^:dynamic *HP*)


(def prompt "> ")
(def player-streams (ref {}))
(def players-inventory (ref {}))

(defn carrying?
  [thing]
  (some #{(keyword thing)} (@*inventory*))
  )
