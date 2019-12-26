(ns mire.commands
  (:use [mire.rooms :only [rooms room-contains?]]
        [mire.player])
  (:use [clojure.string :only [join]]))

(def isBusy?Players [])

(def object-game (hash-set "1" "2" "3"))

(defn changeStatus
  [namePlayer1 namePlayer2 movePlayer1]

  (def isBusy?Players (conj isBusy?Players {:namePlayer1 namePlayer1, :namePlayer2 namePlayer2, :movePlayer1 movePlayer1}))
)

(defn- move-between-refs
  "Move one instance of obj between from and to. Must call in a transaction."
  [obj from to]
  (alter from disj obj)
  (alter to conj obj))

(defn- move-delete
  [obj from]
  (alter from disj obj))




;; Command functions

(defn look
  "Get a description of the surrounding environs and its contents."
  []

  (str (:desc @*current-room*)
       "\r\nExits: " (keys @(:exits @*current-room*)) "\r\n"
       (str (join "\r\n" (map #(str "There is " % " here.\r\n")
                           @(:items @*current-room*)))
            (join "\r\n" (map #(str "Player is " % " here.\r\n")
                           @(:inhabitants @*current-room*)))

;;             (doseq [namePlayer1 (map :namePlayer1 isBusy?Players)
;;                       namePlayer2 (map :namePlayer2 isBusy?Players)]
;;                   (join "\r\n" (str "Playing : " namePlayer1 " - " namePlayer2))
;;             )

;;             (join "\r\n" (map #(str "HP is " % " .\r\n") [(str @*HP*)]))
;;             (join "\r\n" (str "HP is " @*HP* " .\r\n"))
;;             "HP is " @*HP* " .\r\n"
;;             "isHeBusy? " @*isHeBusy?* " .\r\n"
       )

       (doseq [namePlayers isBusy?Players]
         (println "Playing : " (namePlayers :namePlayer1) " - " (namePlayers :namePlayer2))
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
             (look))
        "You can't go that way."))
    (if target                                                            ;; Иначе преходим в комнату
       (do
         (move-between-refs *player-name*
                            (:inhabitants @*current-room*)
                            (:inhabitants target))
         (ref-set *current-room* target)
         (look))
    "You can't go that way.")))))

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
  (if (= #{(keyword thing)} @( :lock @*current-room*))                              ;;Если вещь это ключ от замка, то ты
   (str "Here you cannot throw "(seq  @( :lock @*current-room*)))                         ;; то ты ее не выбросишь:)
  (dosync
   (if (carrying? thing)
     (do (move-between-refs (keyword thing)
                            *inventory*
                            (:items @*current-room*))
         (str "You dropped the " thing "."))
     (str "You're not carrying a " thing ".")))))

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

(defn sayWho
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
;; =============================================================================================================
(defn rps-game                                              ;; Игра Камень-Ножницы-Бумага
  "Rock-Paper-Scissors game."                                ;; Это типа ее описание
  []                                                        ;; Аргументов нет(А что передавать можно то?)
  (println "This is Rock-Paper-Scissors game")              ;; Говорим, что это за игра

  (changeStatus *player-name* "Laptop")

  (def rps [:rock :paper :scissors])                        ;; Объявляем вектор из 3х элементов игры
  (def laptop                                               ;; Здесь хранится ход ноута(системы, или бота, или другого игрока)
    (.indexOf rps (rand-nth rps))                           ;; получаем индекс(с 0) рандомного элемента массива элементов игры(система ходит рандомно), т.е как бы система делает ход
  )

  (println "Laptop is ready. Your move(1 - rock ; 2 - paper ; 3 - scissors) : ")  ;; Говорим о том, что система сделала ход. Пора и нам. В скобках обозначения). Ход делается нажатием клавиш 1,2 или 3 и Enter(как подтвердить)

  (def your-move
    (str (first (read-line)))                               ;; Нажимаем клавиши, делая ход
  )

  (if (= your-move "1") (def your-move :rock))              ;; Анализируем результаты. Такое сравнение сделано, если кто-то захочет вводить не цифры, а буквы, слова и т.д. Так изменять при таких условиях легче(мне кажется).
  (if (= your-move "2") (def your-move :paper))             ;; Вообщем, здесь получаем наш элемент массива.
  (if (= your-move "3") (def your-move :scissors))

  (def your-move (.indexOf rps your-move))                  ;; Здесь получаем индекс нашего элемента массива(так сравниваем числа для определения победителя).

  (def result (- laptop your-move))                         ;; Переменная результата

  (println "Laptop -> " (rps laptop) "\r\n You -> " (rps your-move) "\r\n")    ;; Узнаем кто что поставил

  (if (or (= result 1) (= result -2)) (println "Laptop is WIN."))              ;; Если то, что поставила система дальше по списку, чем наш элемент(т.е result=1), то система победила. И, если result=-2(случай краевых элементов), то тоже
  (if (or (= result -1) (= result 2)) (println "You is WIN."))                 ;; Аналогично, просто меняем знаки, и тогда мы победили
  (if (= result 0) (println "Play again."))                                    ;; Если ничья, то играем заново
  (if (= result 0) (rps-game))                                                 ;; Пока кто-то не победит
)
;; ///////////////////////////////////////////////////////////////////////////////////////////////
(defn rps2-game                                              ;; Игра Камень-Ножницы-Бумага2
  "Rock-Paper-Scissors game with"                                ;; Это типа ее описание
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
    (println "You need play game. Format(N = 1(rock) or 2(paper) or 3(scissors)) : play N !!!")
    (println prompt)
  )
)
;;=================================
(defn exchange-inventory
  "Discard all"
  [winPlayer losePlayer]

  (def result (str winPlayer " is WINER."))
  (println result)



)
;;=================================
(defn result-game
  "End Game"
  [movePlayer2]

  (def vector-object-game (apply vector object-game))
  (def object-game-words ["rock" "paper" "scissors"])
  (def indexThisGame (.indexOf (map :namePlayer2 isBusy?Players) *player-name*))
  (def thisGame (isBusy?Players indexThisGame))
  (def movePlayer1 (thisGame :movePlayer1))

  (println (thisGame :namePlayer1) " -> " (object-game-words (.indexOf vector-object-game movePlayer1)) "\r\n")
  (println *player-name* " -> " (object-game-words (.indexOf vector-object-game movePlayer2)) "\r\n")

  (def result (- (.indexOf vector-object-game movePlayer1) (.indexOf vector-object-game movePlayer2)))                         ;; Переменная результата
;;   (if (or (= result 1) (= result -2)) (def result (str (thisGame :namePlayer1) " is WIN.")))              ;; Если то, что поставила система дальше по списку, чем наш элемент(т.е result=1), то система победила. И, если result=-2(случай краевых элементов), то тоже
;;   (if (or (= result -1) (= result 2)) (def result (str *player-name* " is WIN.")))                 ;; Аналогично, просто меняем знаки, и тогда мы победили
  (if (or (= result 1) (= result -2)) (exchange-inventory (thisGame :namePlayer1) *player-name*))              ;; Если то, что поставила система дальше по списку, чем наш элемент(т.е result=1), то система победила. И, если result=-2(случай краевых элементов), то тоже
  (if (or (= result -1) (= result 2)) (exchange-inventory *player-name* (thisGame :namePlayer1)))                 ;; Аналогично, просто меняем знаки, и тогда мы победили
  (if (= result 0) (def result (str "Draw. Each remained at his own.")))                                    ;; Если ничья, то каждый остается при своем и игра заканчивается.

  (println result)

  (binding [*out* (player-streams (thisGame :namePlayer1))]
    (println (thisGame :namePlayer1) " -> " (object-game-words (.indexOf vector-object-game movePlayer1)) "\r\n")
    (println *player-name* " -> " (object-game-words (.indexOf vector-object-game movePlayer2)) "\r\n")
    (println result)

    (println prompt)
  )

  (def isBusy?Players (apply merge (subvec isBusy?Players 0 indexThisGame) (subvec isBusy?Players (inc indexThisGame) (count isBusy?Players))))
  (println isBusy?Players)
)
;;=================================
(defn play-
  "Player answer in the rps game"
  [moveplayer]

  (def move2 moveplayer)

  (if (contains? (apply hash-set (map :namePlayer1 isBusy?Players)) *player-name*)
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
  [name2player]

  (def mapPlayers (apply hash-set
                         (apply merge (map :namePlayer1 isBusy?Players) (map :namePlayer2 isBusy?Players))
                  )
  )

  (if (or (not (contains? @(:inhabitants @*current-room*) name2player))
          (contains? mapPlayers name2player)
          (= name2player *player-name*))
    (println "The player is not this room or he is busy or he not exist or you input your name. Try later.\r\n")
    (rps2-game name2player)
  )
)

;;=================================
;;============work=================


(defn finished "for work" []
  (println "Thanks for working!\r\n")
    )

(defn forthTask "for work" []
  (println "How many members in EXO")
  (println "a-8, b-11, c-9\r\n")
  (def ans(read-line))
  (case ans
   "c" (finished),
   "b" "Wrong!"
   "a" "Wrong!")
  (println "Here's it, if you wish to earn more just ask for 'work'...\r\n")
  )

(defn thirdTask "for work" []
  (println "How many regions in Russia")
  (println "a-84, b-85, c-76\r\n")
  (def ans(read-line))
  (case ans
      "b" (forthTask),
      "c" "Wrong!"
      "a" "Wrong!")
    )

(defn secondTask "for work" []
  (println "What is 5+5*3-1+9/3?")
  (println "a-17, b-12, c-22\r\n")
  (def ans(read-line))
  (case ans
    "c" (thirdTask),
    "a" "Wrong!"
    "b" "Wrong!")
  )

(defn firstTask "for work" []
  (println "How many colours in rainbow?")
  (println "a-7, b-5, c-6\r\n")
  (def ans(read-line))
  (case ans
    "a"  (secondTask),
    "b" "Wrong!"
    "c" "Wrong!"
    )
  )

  (defn work "This is work" []
    (println "yes: I need to work")
    (println "no: I don't want to work")
    (println "Write your answer: ")
    (def answer (read-line))
    (case answer
        "yes" (firstTask),
        "no" "Ok, maybe next time"
    )
  )


;;==================================
;;==========end of work=============

;;==================================
;; Command data

(def commands {"move" move
               "north" (fn [] (move :north)),
               "south" (fn [] (move :south)),
               "east"  (fn [] (move :east)),
               "west"  (fn [] (move :west)),
               "grab" grab
               "discard" discard
               "inventory" inventory
               "detect" detect
               "look" look
               "say" say
               "tell" sayWho
               "help" help
               "play" provPlayer
               "play-" play-
               "ei" exchange-inventory
               "work" work
               })

;; Command handling

(defn execute
  "Execute a command that is passed to us."
  [input]
  (try (let [[command & args] (.split input " +")]
          (def mapPlayers (apply hash-set (apply merge (map :namePlayer1 isBusy?Players) (map :namePlayer2 isBusy?Players))))
          (if (and (contains? mapPlayers *player-name*) (not= command "play-"))
              (println "You need ending the game.")
              (do
                (apply (commands command) args)
              ))
        )
       (catch Exception e
         (.printStackTrace e (new java.io.PrintWriter *err*))
         "You can't do that!")))
