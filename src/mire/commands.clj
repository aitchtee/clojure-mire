(ns mire.commands
  (:use [mire.rooms :only [rooms room-contains? room-contains-gold?]]
        [mire.player]
        [mire.data :only [players-inventory]]
        [mire.maniac]
        [mire.utilities]
        [mire.emojiList]
        [clojure.java.io])
  (:use [clojure.string :only [join]])

  (:require
  		[clojure.data.json 						 :as json]
    [immutant.web             :as web]
    [immutant.web.async       :as async]
    [immutant.web.middleware  :as web-middleware]
    [compojure.route          :as route]
    [environ.core             :refer (env)]
    [compojure.core           :refer (ANY GET defroutes)]
    [ring.util.response       :refer (response redirect content-type)]
  )
  (:import 
		(java.lang String Thread)
		(java.net InetAddress ServerSocket  Socket SocketException)
    (java.io InputStreamReader OutputStream  PrintWriter StringReader StringWriter IOException PipedInputStream PipedOutputStream)

  )
)

(def PlayingPlayers [])

(def object-game (vector "rock" "paper" "scissors"))

(defn changeStatus
  [namePlayer1 namePlayer2 movePlayer1]

  (def PlayingPlayers (conj PlayingPlayers {:namePlayer1 namePlayer1, :namePlayer2 namePlayer2, :movePlayer1 movePlayer1}))
)

;; Command functions

(defn look
  "Get a description of the surrounding environs and its contents."
  []
  ( if (= *player-channel* 0)
  	( do
		  (println "You : " {:id *player-id*, :name *player-name*})
		  (str (:desc @*current-room*)
		       "\r\nExits: " (keys @(:exits @*current-room*)) "\r\n"
		       (str (join "\r\n" (map #(str "There is " % " here.\r\n")
		                           @(:items @*current-room*)))
		       
		            (join "\r\n" (map #(str "Player is " {:id (% :id), :name (% :name)} " here.\r\n")
		                           (filter #(contains? @(:inhabitants @*current-room*) (% :name)) players-inventory)
		                              ))
		            (join (str "GOLD " @(:gold @*current-room*) " here.\r\n"))
		       )

		       (doseq [namePlayers PlayingPlayers]
		         (println "Playing : " (namePlayers :namePlayer1) " - " (namePlayers :namePlayer2))
		       )

		       "Maniacs " (join "\r\n" (map #(str % " here.\r\n")
		                           @(:maniacs @*current-room*)))

	  	)
		 )
		 (json/write-str 
		 	(conj {} 
		 		[:your_id *player-id*]
		 		[:your_name *player-name*]
		 		[:name (:name @*current-room*)] 
		 		[:desc (:desc @*current-room*)] 
		 		[:exits (keys @(:exits @*current-room*))]
		 		[:inhabitants  
		 			(for [x (filter #(contains? @(:inhabitants @*current-room*) (% :name)) players-inventory)]
		 				(conj {} [ :id (:id x) ] [:name (:name x)])
		 			)
		 		]
		 	;[:maniacs @(:maniacs @*current-room*) ]
		 		[:playing   
		 			(doseq [namePlayers PlayingPlayers]
		       	(conj {} 
		        	[:namePlayer1 (namePlayers :namePlayer1)] 
		         	[:namePlayer2 (namePlayers :namePlayer2)]
		        )
		      )
		    ]
		 		[:items @(:items @*current-room*)]
		 		[:gold @(:gold @*current-room*)]
		 	)  
  	)
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
    (if (or (= thing "coin") (= thing "bagmoney5") (= thing "bagmoney10") (= thing "bagmoney20"))
      (if (room-contains-gold? @*current-room* thing)
        (do
          (case thing
            "coin" (swap! *money* inc)
            "bagmoney5" (swap! *money* + 5)
            "bagmoney10" (swap! *money* + 10)
            "bagmoney20" (swap! *money* + 20)
          )
          (if (= ((keyword thing) @(:gold @*current-room*)) 1)
            (alter (:gold @*current-room*) dissoc (keyword thing))
            (do
              (def temp-gold ((keyword thing) @(:gold @*current-room*)))
              (alter (:gold @*current-room*) dissoc (keyword thing))
              (alter (:gold @*current-room*) assoc (keyword thing) (- temp-gold 1))
            )
          )
          (str "You picked up the " thing ".")
        )
        (str "There isn't any " thing " here.")
      )
      (if (room-contains? @*current-room* thing)
        (do
          (move-between-refs (keyword thing)
                             (:items @*current-room*)
                             *inventory*)
          (str "You picked up the " thing ".")
        )
        (str "There isn't any " thing " here.")
      )
    )
  )
)

(defn seemoney
  "See your money"
  []
  (str (join "\r\n" (map #(str "Money is " % " .\r\n") [(str @*money*)])))
)

(defn discard
  "Put something down that you're carrying."
  [thing]
  (if (= #{(keyword thing)} @( :lock @*current-room*))                              ;;Если вещь это ключ от замка, то ты
    (str "Here you cannot throw "(seq  @( :lock @*current-room*)))                         ;; то ты ее не выбросишь:)
    (dosync
      (if (or (= thing "coin") (= thing "bagmoney5") (= thing "bagmoney10") (= thing "bagmoney20"))
        (case thing
          "coin" (if (>= @*money* 0)
                    (do
                      (swap! *money* dec)
                      (if (room-contains-gold? @*current-room* thing)
                        (def temp-gold ((keyword thing) @(:gold @*current-room*)))
                        (def temp-gold 0)
                      )
                      (alter (:gold @*current-room*) assoc (keyword thing) (+ temp-gold 1))
                      (str "You dropped the " (keyword thing) ".")
                    )
                    (str "Not enough money!")
                  )
          "bagmoney5" (if (>= @*money* 4)
                        (do
                          (swap! *money* - 5)
                          (if (room-contains-gold? @*current-room* thing)
                            (def temp-gold ((keyword thing) @(:gold @*current-room*)))
                            (def temp-gold 0)
                          )
                          (alter (:gold @*current-room*) assoc (keyword thing) (+ temp-gold 1))
                          (str "You dropped the " (keyword thing) ".")
                        )
                        (str "Not enough money!")
                      )
          "bagmoney10" (if (>= @*money* 9)
                         (do
                           (swap! *money* - 10)
                           (if (room-contains-gold? @*current-room* thing)
                             (def temp-gold ((keyword thing) @(:gold @*current-room*)))
                             (def temp-gold 0)
                           )
                           (alter (:gold @*current-room*) assoc (keyword thing) (+ temp-gold 1))
                           (str "You dropped the " (keyword thing) ".")
                         )
                         (str "Not enough money!")
                       )
          "bagmoney20" (if (>= @*money* 19)
                         (do
                           (swap! *money* - 20)
                           (if (room-contains-gold? @*current-room* thing)
                             (def temp-gold ((keyword thing) @(:gold @*current-room*)))
                             (def temp-gold 0)
                           )
                           (alter (:gold @*current-room*) assoc (keyword thing) (+ temp-gold 1))
                           (str "You dropped the " (keyword thing) ".")
                         )
                         (str "Not enough money!")
                       )
        )
        (if (carrying? thing)
          (do (move-between-refs (keyword thing)
                                 *inventory*
                                 (:items @*current-room*))
              (str "You dropped the " thing ".")
          )
          (str "You're not carrying a " thing ".")
        )
      )
    )
  )
)

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
    	(def other-player-channel (connected_name_channel inhabitant))
    	(if (= other-player-channel 0)
	      (binding [*out* (player-streams inhabitant)]
	        (println *player-name* " : " message " " prompt)
	      )
	      (if (@connected_name_channel inhabitant)
	      	(do
				  	(async/send! (connected_name_channel inhabitant) (str *player-name* " : " message " " prompt))
				  	(flush)
	      	)
	      )
    	)
    )
    (str "You said " message)))

(defn help
  "Show available commands and what they do."
  []
  (join "\r\n" (map #(str (key %) ": " (:doc (meta (val %))))
                      (dissoc (ns-publics 'mire.commands)
                              'execute 'commands))))
;; ///////////////////////////////////////////////////////////////////////////////////////////////
(defn rps2-game                                              ;; Игра Камень-Ножницы-Бумага2
  "Get move 1st player"                                ;; Это типа ее описание
  [name2player your-move]

  (def nameGame "This is Rock-Paper-Scissors game")
  (print nameGame) (print prompt) (flush) (.flush *out* )                                     ;; Говорим, что это за игра

  ; (println "Your move(1 - rock ; 2 - paper ; 3 - scissors) : ") (print prompt) (flush) (.flush *out* )
  ; (def moveplayer (read-line))
  ; (def moveplayer -1)

  (def moveplayer-object your-move)

  (if (not (contains? (apply hash-set object-game) moveplayer-object))
	  (while (not (contains? (apply hash-set object-game) moveplayer-object))
	    (do
	      (print "No correct move!") (print prompt) (flush) (.flush *out* )
	      (def moveplayer-object (read-line))
	      (println moveplayer-object)
	  	)
	  )  	
  )

  (changeStatus *player-name* name2player moveplayer-object)

;=========================
		(if (= (@connected_name_channel name2player) 0)
	  (binding [*out* (player-streams name2player)]
	    (println "Player " *player-name* " wants play game with you!") (print prompt) (flush) (.flush *out* )
	    (println "You need play game. Format(N = rock or paper or scissors) : play- N !!!")
	    (print prompt)
	  )
	  (do
	  	(async/send! (@connected_name_channel name2player) (str "Player " *player-name* " wants play game with you!"))
	  	(async/send! (@connected_name_channel name2player) "You need play game. Format(N = rock or paper or scissors) : play- N !!!")
	  )
		)
;-------------------------
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
  [movePl2]

  (def indexThisGame (.indexOf (map :namePlayer2 PlayingPlayers) *player-name*))
  (def thisGame (PlayingPlayers indexThisGame))
  (def movePlayer1 (+ (.indexOf object-game (thisGame :movePlayer1)) 1))
  (def movePlayer2 (+ (.indexOf object-game movePl2) 1))

  (println (thisGame :namePlayer1) " -> " (thisGame :movePlayer1) "\r\n")
  (println *player-name* " -> " movePl2 "\r\n")

  (def result (- movePlayer1 movePlayer2))                         ;; Переменная результата
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
  (if (= (@connected_name_channel (thisGame :namePlayer1)) 0)
	  (binding [*out* (player-streams (thisGame :namePlayer1))]
	    (println (thisGame :namePlayer1) " -> " (thisGame :movePlayer1) "\r\n")
	    (println *player-name* " -> " movePl2 "\r\n")
	    (println result)
	  )
	  (do
	    (async/send! (@connected_name_channel (thisGame :namePlayer1)) 
	    		(str (thisGame :namePlayer1) " -> " (thisGame :movePlayer1) "\r\n"))
	    (async/send! (@connected_name_channel (thisGame :namePlayer1)) (str *player-name* " -> " movePl2 "\r\n"))
	    (async/send! (@connected_name_channel (thisGame :namePlayer1)) result)
	  )
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
       (while (not (contains? (apply hash-set object-game) move2))
        (do
          (println "No correct move!") (print prompt) (flush) (.flush *out* )
          (def move2 (read-line))
        ))
       (result-game move2)
     )
  )
)
;;=================================
(defn provPlayer
  "Play test"
  [id2player your-move]

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
        (rps2-game name2player your-move)
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
;;===================================

(defn finished "for work" []
  (println  "Thanks for working!\r\n") (print prompt) (flush) (.flush *out* )
  (println "You've earn " @*money*) (print prompt) (flush) (.flush *out* )
    )

(defn forthTask "for work" []
  (println "How many members in EXO") (print prompt) (flush) (.flush *out* )
  (println "a-8, b-11, c-9\r\n") (print prompt) (flush) (.flush *out* )
  (def ans(read-line))
  (case ans
   "c"(do (swap! *money* + 1) (println @*money*) (print prompt) (flush) (.flush *out* ) (finished)) ,
   "b" (do (println "Wrong!") (println prompt) (print prompt) (flush) (.flush *out* ) (finished)),
   "a" (do (println "Wrong!") (println prompt) (print prompt) (flush) (.flush *out* ) (finished)))
  (println "Here's it, if you wish to earn more just ask for 'work'...\r\n") (print prompt) (flush) (.flush *out* )
  )

(defn thirdTask "for work" []
  (println "How many regions in Russia") (print prompt) (flush) (.flush *out* )
  (println "a-84, b-85, c-76\r\n") (print prompt) (flush) (.flush *out* )
  (def ans(read-line))
  (case ans
      "b"(do (swap! *money* + 1) (println @*money*) (print prompt) (flush) (.flush *out* ) (forthTask)),
      "c" (do (println "Wrong!") (print prompt) (flush) (.flush *out* ) (forthTask)),
      "a" (do (println "Wrong!") (print prompt) (flush) (.flush *out* ) (forthTask)))
    )

(defn secondTask "for work" []
  (println "What is 5+5*3-1+9/3?") (print prompt) (flush) (.flush *out* )
  (println "a-17, b-12, c-22\r\n") (print prompt) (flush) (.flush *out* )
  (def ans(read-line))
  (case ans
    "c" (do (swap! *money* + 1) (println @*money*) (print prompt) (flush) (.flush *out* ) (thirdTask)),
    "a" (do (println "Wrong!") (print prompt) (flush) (.flush *out* ) (thirdTask)),
    "b" (do (println "Wrong!") (print prompt) (flush) (.flush *out* ) (thirdTask)))
  )

(defn firstTask "for work" []
  (println "How many colours in rainbow?") (print prompt) (flush) (.flush *out* )
  (println "a-7, b-5, c-6\r\n") (print prompt) (flush) (.flush *out* )
  (def ans(read-line))
  (case ans
    "a" (do (swap! *money* + 1) (println @*money*) (print prompt) (flush) (.flush *out* ) (secondTask)),
    "b" (do (println "Wrong!") (print prompt) (flush) (.flush *out* ) (secondTask)),
    "c" (do (println "Wrong!") (print prompt) (flush) (.flush *out* ) (secondTask))
    )
  )

  (defn work "This is work" []

  (if (= (:name @*current-room*) (keyword "work")) 
  ( do 
    (println "yes: I need to work") (print prompt) (flush) (.flush *out* )
    (println "no: I don't want to work") (print prompt) (flush) (.flush *out* )
    (println "Write your answer: ") (print prompt) (flush) (.flush *out* )
    (def answer (read-line))
    (case answer
        "yes" (firstTask),
        "no" "Ok, maybe next time"
    )
  )
  (do
  (println "You aren't at work. Please, go to work to use this command") (print prompt) (flush) (.flush *out* )
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
               "seemoney" seemoney
               "discard" discard
               "inventory" inventory
               "detect" detect
               "look" look
               "say" say
               "help" help
               "play" provPlayer
               "play-" play-
               "gen-maniac" gen-maniac
               "curr_emoji" curr_emoji
               "list_emoji" list_emoji
               "set_emoji" set_emoji
               "work" work
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
