(ns mire.player)

(def ^:dynamic *current-room*)
(def ^:dynamic *current-emoji*)
(def ^:dynamic *inventory*)
(def ^:dynamic *emoji-available*)
(def ^:dynamic *player-name*)

(def prompt "> ")
(def player-streams (ref {}))

(defn carrying?
  [thing]
  (some #{(keyword thing)} @*inventory*))
