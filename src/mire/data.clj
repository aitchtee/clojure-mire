(ns mire.data)

(def idPlayer 0)
(def players-inventory [])

(defn newPlayer
  [idPlayer player-name]
  (def idPlayer (inc idPlayer))
  (def players-inventory (conj players-inventory {:id (inc idPlayer), :name player-name, :inventory (ref #{})}))
)
