(ns mire.commands
  (:use [mire.rooms :only [rooms room-contains?]]
        [mire.player])
  (:use [clojure.string :only [join]]))

(defn- move-between-refs
  "Move one instance of obj between from and to. Must call in a transaction."
  [obj from to]
  (alter from disj obj)
  (alter to conj obj))

;; Command functions

(defn look
  "Get a description of the surrounding environs and its contents."
  []
  (str (:desc @*current-room*)
        "\n\rExits: " (keys @(:exits @*current-room*)) "\n\r"
        (join "\n\r" (map #(str "There is " % " here.\n\r")
                           @(:items @*current-room*)))
        (join "\n\r" (map #(str "Player " % " is here.\n\r")
                           @(:inhabitants @*current-room*)))
  ))

(defn move
  "\"♬ We gotta get out of this place... ♪\" Give a direction."
  [direction]
  (dosync
    (let [target-name ((:exits @*current-room*) (keyword direction))
         target (@rooms target-name)]
      (if target
            (do
              (move-between-refs *player-name*
                                (:inhabitants @*current-room*)
                                (:inhabitants target))
              (ref-set *current-room* target)
              (look))
        "You can't go that way.")
    )))

(defn grab
  "Pick something up."
  [thing]
  (dosync
   (if (room-contains? @*current-room* thing)
     (do (move-between-refs (keyword thing)
                            (:items @*current-room*)
                            *inventory*)
         (str "You picked up the " thing "."))
     (str "There isn't any " thing " here."))))

(defn discard
  "Put something down that you're carrying."
  [thing]
  (dosync
   (if (carrying? thing)
     (do (move-between-refs (keyword thing)
                            *inventory*
                            (:items @*current-room*))
         (str "You dropped the " thing "."))
     (str "You're not carrying a " thing "."))))

(defn inventory
  "See what you've got."
  []
  (str "You are carrying:\n\r"
       (join "\n\r" (seq @*inventory*))))

(defn detect
  "If you have the detector, you can see which room an item is in."
  [item]
  (if (@*inventory* :detector)
    (if-let [room (first (filter #((:items %) (keyword item))
                                 (vals @rooms)))]
      (str item " is in " (:name room))
      (str item " is not in any room."))
    "You need to be carrying the detector for that."))

(defn say
  "Say something out loud so everyone in the room can hear."
  [& words]
  (let [message (join " " words)]
    (doseq [inhabitant (disj @(:inhabitants @*current-room*) *player-name*)]
      (binding [*out* (player-streams inhabitant)]
        (println *player-name*" said: " message)
        (println prompt)))
    (str "You said " message)))

(defn help
  "Show available commands and what they do."
  []
  (join "\n" (map #(str (key %) ": " (:doc (meta (val %))))
                      (dissoc (ns-publics 'mire.commands)
                              'execute 'commands))))

(defn cleanup []
  "Drop all inventory."
  (dosync
   (doseq [item @*inventory*]
     (discard item))
  ;  (commute player-streams dissoc *player-name*)
  ;  (commute (:inhabitants @*current-room*)
  ;           disj *player-name*)
  ))

;; Command data

(def commands {"move" move,
               "north" (fn [] (move :north)),
               "south" (fn [] (move :south)),
               "east" (fn [] (move :east)),
               "west" (fn [] (move :west)),
               "grab" grab
               "discard" discard
               "inventory" inventory
               "detect" detect
               "look" look
               "say" say
               "help" help
               "cleanup" cleanup})

;; Command handling

(defn execute
  "Execute a command that is passed to us."
  [input]
  (try (let [[command & args] (.split input " +")]
         (apply (commands command) args))
       (catch Exception e
         (.printStackTrace e (new java.io.PrintWriter *err*))
         "You can't do that!")))
