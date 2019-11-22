(ns mire.server
  (:use [mire.player]
        [mire.commands :only [discard look execute changeStatus isBusy?Players]]
        [mire.rooms :only [add-rooms rooms]])
  (:use [clojure.java.io :only [reader writer]]
        [server.socket :only [create-server]]))

(defn- cleanup
  [namePlayer]
  "Drop all inventory and remove player from room and player list."
  (dosync
   (doseq [item @*inventory*]
     (discard item))
   (commute player-streams dissoc *player-name*)
   (commute (:inhabitants @*current-room*)
            disj *player-name*)))

(defn- get-unique-player-name [name]
  (if (@player-streams name)
    (do (print "That name is in use; try again: ")
        (flush)
        (recur (read-line)))
    name))

(defn- mire-handle-client [in out]
  (binding [*in* (reader in)
            *out* (writer out)
            *err* (writer System/err)]

    ;; We have to nest this in another binding call instead of using
    ;; the one above so *in* and *out* will be bound to the socket
    (print "\nWhat is your name? ") (flush)

    (def player-name (get-unique-player-name (read-line)) )    ;; Устанавливаю переменной player-name имя игрока, введеное в консоли
    (def hp 100)
    (binding [
              *player-name* player-name
              *current-room* (ref (@rooms :start))
              *inventory* (ref #{})
              *HP* (ref hp)
              ]
      (dosync
       (commute (:inhabitants @*current-room*) conj *player-name*)
       (commute player-streams assoc *player-name* *out*)
;;        (commute players-inventory conj *inventory*)
      )

      (println (look)) (print prompt) (flush)

      (try (loop [input (read-line)]
             (when input
               (println (execute input))
               (.flush *err*)
               (print prompt) (flush)
               (recur (read-line))))
           (finally (cleanup))))))

(defn -main
  ([port dir]
     (add-rooms dir)
     (defonce server (create-server (Integer. port) mire-handle-client))
     (println "Launching Mire server on port" port))
  ([port] (-main port "resources/rooms"))
  ([] (-main 3333)))
