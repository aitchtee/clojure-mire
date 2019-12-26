(ns mire.commands
  (:use [mire.rooms :only [rooms room-contains?]]
        [mire.player]
        [mire.data :only [players-inventory]]
        [mire.maniac]
        [mire.utilities]
        [mire.emojiList])
  (:use [clojure.string :only [join]]))

(def PlayingPlayers [])

(def object-game (hash-set "1" "2" "3"))

(defn changeStatus
  [namePlayer1 namePlayer2 movePlayer1]

  (def PlayingPlayers (conj PlayingPlayers {:namePlayer1 namePlayer1, :namePlayer2 namePlayer2, :movePlayer1 movePlayer1}))
)

;; Command functions

(defn look
  "Get a description of the surrounding environs and its contents."
  []
  (println "You : " {:id *player-id*, :name *player-name*})
  (str (:desc @*current-room*)
       "\r\nExits: " (keys @(:exits @*current-room*)) "\r\n"
       (str (join "\r\n" (map #(str "There is " % " here.\r\n")
                           @(:items @*current-room*)))
       
            (join "\r\n" (map #(str "Player is " {:id (% :id), :name (% :name)} " here.\r\n")
                           (filter #(contains? @(:inhabitants @*current-room*) (% :name)) players-inventory)
                              ))
       )

       (doseq [namePlayers PlayingPlayers]
         (println "Playing : " (namePlayers :namePlayer1) " - " (namePlayers :namePlayer2))
       )

       "Maniacs " (join "\r\n" (map #(str % " here.\r\n")
                           @(:maniacs @*current-room*)))
  )
)

(defn move
  "\"♬ We gotta get out of this place... ♪\" Give a direction."
  [direction]
  (dosync
   (let [target-name ((:exits @*current-room*) (keyword direction))      ;;получить все выходы в исходной комнате и обозначить путь
         target (@rooms target-name)]                                    ;; получение комнаты из списка
     (if (not= @( :lock target) #{(some @( :lock target) @*inventory*)}) ;; Если замок не равен предменту из инвентаря то
        (if (not= @( :lock target) #{})                                  ;;     (Если замок не равен пустане то
           ( str "LOCK!!! Find an " ( seq @( :lock target)) " to pass " )       ;;        выводим сообщение )
        (if target                                                          ;;   Иначе переходим в комнату
           (do
             (move-between-refs *player-name*
                                (:inhabitants @*current-room*)
                                (:inhabitants target))
             (ref-set *current-room* target)

             (maniac-fight)
             (look))
        "You can't go that way."))
    (if target                                                            ;; Иначе преходим в комнату
       (do
         (move-between-refs *player-name*
                            (:inhabitants @*current-room*)
                            (:inhabitants target))
         (ref-set *current-room* target)
         (maniac-fight)
         (look))
    "You can't go that way.")))))

(defn grab
  "Pick something up."
  [thing]
  (dosync
   (if (room-contains? @*current-room* thing)
     (if (= thing "money")
      (do
      	; (def player-money ((first (filter #(= (% :name) *player-name*) players-inventory)) :money))
      	; (+ @player-money 1)
      	(swap! *money* + 1)
      	(alter (:items @*current-room*) disj (keyword thing))
        (str "You picked up the money.")
      )
      (do
        (move-between-refs (keyword thing)
                            (:items @*current-room*)
                            *inventory*)
        (str "You picked up the " thing ".")
      )
     )
     (str "There isn't any " thing " here."))))

(defn discard
  "Put something down that you're carrying."
  [thing]
  (if (= #{(keyword thing)} @( :lock @*current-room*))                              ;;Если вещь это ключ от замка, то ты
   (str "Here you cannot throw "(seq  @( :lock @*current-room*)))                         ;; то ты ее не выбросишь:)
  (dosync
		(if (= thing "money")
			(if (> @*money* 0)
				(do 
					(swap! *money* - 1)
				  (alter (:items @*current-room*) conj (keyword thing))
				 	(str "You dropped the money.")
				)
				(str "No money!")
			)
			(if (carrying? thing)
				(do 
					(move-between-refs (keyword thing)
				                      	*inventory*
				                      	(:items @*current-room*))
				  (str "You dropped the " thing ".")
				)
				(str "You're not carrying a " thing ".")
			)
		)
  )))

(defn inventory
  "See what you've got."
  []
  (str "You are carrying:\r\n"
       (join "\r\n" (seq @*inventory*))))

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
        (println *player-name* " : " message)
        (println prompt)))
    (str "You said " message)))

(defn tell
  "Say something out loud so everyone in the room can hear."
  [namePlayer]

  (println "Input text message for player " namePlayer " : ")
    (def message (read-line))

      (binding [*out* (player-streams namePlayer)]
        (println "Message from " *player-name* " : " message)
        (println prompt)
        )
    (str "You message was send player " namePlayer)
)

(defn help
  "Show available commands and what they do."
  []
  (join "\r\n" (map #(str (key %) ": " (:doc (meta (val %))))
                      (dissoc (ns-publics 'mire.commands)
                              'execute 'commands))))
;; ///////////////////////////////////////////////////////////////////////////////////////////////
(defn rps2-game                                              ;; Игра Камень-Ножницы-Бумага2
  "Get move 1st player"                                ;; Это типа ее описание
  [name2player]

  (def nameGame "This is Rock-Paper-Scissors game")
  (println nameGame)                                        ;; Говорим, что это за игра

  (println "Your move(1 - rock ; 2 - paper ; 3 - scissors) : ")
  (def moveplayer (read-line))

  (while (not (contains? object-game moveplayer))
    (do
      (println "No correct move!")
      (def moveplayer (read-line))
  ))

  (changeStatus *player-name* name2player moveplayer)

  (binding [*out* (player-streams name2player)]
    (println "Player " *player-name* " wants play game with you!")
    (println "You need play game. Format(N = 1(rock) or 2(paper) or 3(scissors)) : play- N !!!")
    (println prompt)
  )
)
;;=================================
(defn let-fly-inventory
  "Discard all"
  [losePlayer]
  (def player-inventory ((first (filter #(= (% :name) losePlayer) players-inventory)) :inventory))
  (dosync
    (doseq [thing @player-inventory]
        (move-between-refs thing
                           player-inventory
                           (:items @*current-room*))
    )
    (do
      (move-between-refs *player-name*
                      (:inhabitants @*current-room*)
                      (:inhabitants (@rooms :start)))
      (ref-set *current-room* (@rooms :start))
    )
  )
  (println "Yes")
)
;;=================================
(defn result-game
  "End Game"
  [movePlayer2]

  (def vector-object-game (apply vector object-game))
  (def object-game-words ["rock" "paper" "scissors"])
  (def indexThisGame (.indexOf (map :namePlayer2 PlayingPlayers) *player-name*))
  (def thisGame (PlayingPlayers indexThisGame))
  (def movePlayer1 (thisGame :movePlayer1))

  (println (thisGame :namePlayer1) " -> " (object-game-words (.indexOf vector-object-game movePlayer1)) "\r\n")
  (println *player-name* " -> " (object-game-words (.indexOf vector-object-game movePlayer2)) "\r\n")

  (def result (- (.indexOf vector-object-game movePlayer1) (.indexOf vector-object-game movePlayer2)))                         ;; Переменная результата
;;   (if (or (= result 1) (= result -2)) (def result (str (thisGame :namePlayer1) " is WIN.")))              ;; Если то, что поставила система дальше по списку, чем наш элемент(т.е result=1), то система победила. И, если result=-2(случай краевых элементов), то тоже
;;   (if (or (= result -1) (= result 2)) (def result (str *player-name* " is WIN.")))                 ;; Аналогично, просто меняем знаки, и тогда мы победили
  (if (or (= result 1) (= result -2))
    (do
      (def result (str (thisGame :namePlayer1) " is WIN."))
      (let-fly-inventory *player-name*)
    )
  )              ;; Если то, что поставила система дальше по списку, чем наш элемент(т.е result=1), то система победила. И, если result=-2(случай краевых элементов), то тоже
  (if (or (= result -1) (= result 2))
    (do
      (def result (str *player-name* " is WIN."))
      (let-fly-inventory (thisGame :namePlayer1))
    )
  )                 ;; Аналогично, просто меняем знаки, и тогда мы победили
  (if (= result 0) (def result (str "Draw. Each remained at his own.")))                                    ;; Если ничья, то каждый остается при своем и игра заканчивается.

  (println result)
  (binding [*out* (player-streams (thisGame :namePlayer1))]
    (println (thisGame :namePlayer1) " -> " (object-game-words (.indexOf vector-object-game movePlayer1)) "\r\n")
    (println *player-name* " -> " (object-game-words (.indexOf vector-object-game movePlayer2)) "\r\n")
    (println result)
  )

  (def PlayingPlayers (apply merge (subvec PlayingPlayers 0 indexThisGame) (subvec PlayingPlayers (inc indexThisGame) (count PlayingPlayers))))
)
;;=================================
(defn play-
  "Player answer in the rps game"
  [moveplayer]

  (def move2 moveplayer)

  (if (contains? (apply hash-set (map :namePlayer1 PlayingPlayers)) *player-name*)
    (println "You cannot ahange your choise")

    (let []
       (while (not (contains? object-game move2))
        (do
          (println "No correct move!")
          (def move2 (read-line))
        ))
       (result-game move2)
     )
  )
)
;;=================================
(defn provPlayer
  "Play test"
  [id2player]

  (def id2playerLong (Long/parseLong id2player))

  (def mapPlayers (apply hash-set
                         (apply merge (map :namePlayer1 PlayingPlayers) (map :namePlayer2 PlayingPlayers))
                  )
  )


  (try
    (do
      (def name2player ((first (filter #(= (% :id) id2playerLong) players-inventory)) :name))
      (if (or (not (contains? @(:inhabitants @*current-room*) name2player))
              (contains? mapPlayers name2player)
              (= name2player *player-name*))
        (println "The player is not this room or he is busy or he not exist or you input your name. Try later.\r\n")
        (rps2-game name2player)
      )
    )
    (catch NullPointerException e (println "The player is not exist"))
  )
)

;===================================
;; Emoji functions

(defn- pretty_keyword
	"Print keyword without ':'"
	[keyword]
	(subs (str keyword) 1)
)

(defn curr_emoji
					"get your current emoji"
					[]
					(str @*current-emoji*)
)

(defn list_emoji
				"List available emotions"
				[]
				 (
				 		str "Available emotions:\r\n" (
				 					join "\r\n" (
				 						 map 
				 							pretty_keyword 
				 							@*emoji-available*
				 									
				 				)
				 		)
     )
)

(defn set_emoji
	"Set your current emoji to new value"
	[emoji_in]
	(dosync	
				(if (@*emoji-available* (keyword emoji_in) )
						( do
							(ref-set *current-emoji*  (keyword emoji_in) )
							(str "your current emoji is" (pretty_keyword @*current-emoji*) ) 
						)
						(
									str "You haven't such emoji"
						)
				)
	)
)
;;==================================

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
               "tell" tell
               "help" help
               "play" provPlayer
               "play-" play-
               "gen-maniac" gen-maniac
               "curr_emoji" curr_emoji
               "list_emoji" list_emoji
               "set_emoji" set_emoji
               ;"say_loud" say_loud
               ;"kill-maniac" kill-maniac
               })

;; Command handling

(defn execute
  "Execute a command that is passed to us."
  [input]
  (try (let [[command & args] (.split input " +")]
          (def mapPlayers (apply hash-set (apply merge (map :namePlayer1 PlayingPlayers) (map :namePlayer2 PlayingPlayers))))
          (if (and (contains? mapPlayers *player-name*) (not= command "play-"))
              (println "You need ending the game.")
              (do
                (apply (commands command) args)
              ))
        )
       (catch Exception e
         (.printStackTrace e (new java.io.PrintWriter *err*))
         "You can't do that!")))
