(ns mire.player)

(def ^:dynamic *current-room*)
(def ^:dynamic *inventory*)
(def ^:dynamic *player-name*)
(def ^:dynamic *HP*)
(def ^:dynamic *money*)
;; (def ^:dynamic *isHeBusy?*)


(def prompt "> ")
(def player-streams (ref {}))

(defn carrying?
  [thing]
  (some #{(keyword thing)} @*inventory*))
