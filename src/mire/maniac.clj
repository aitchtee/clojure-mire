(ns mire.maniac
  (:use [mire.rooms :only [rooms]])
  (:use [mire.emojiList]
  						[mire.player]
  						[mire.utilities]
  						[mire.data]
  				 [clojure.string :only [join]]
  	)			 

)  

(def maniacs (ref {}))
(def temp-rooms (ref {}))


(defn get-temp-rooms []
(def room (vals (deref rooms)))

 (def res
    (for [room (vals (deref rooms))]
      (do
        (apply merge {:name (room :name)})
      )
    )
 )
(def res1 (for [x  res]  ( get x 1) ) )
 (def filtered_res	(filter (fn [x] (do
                      (not= x nil)
                      (not= x (keyword "start"))
                    )
            ) res1 ))

  (def temp-rooms filtered_res ) ;(for [x  res]  ( get x 1) )
)


(defn kill-player
  "Discard all"
  []
  (def player-invent ((first (filter #(= (% :name) *player-name*) players-inventory)) :inventory))
  (dosync
    (doseq [thing @player-invent]
        (move-between-refs thing
                           player-invent
                           (:items @*current-room*))
    )
    (do
      (move-between-refs *player-name*
                      (:inhabitants @*current-room*)
                      (:inhabitants (@rooms :start)))
      (ref-set *current-room* (@rooms :start))
    )
  )
)

;;Maniac functions
(defn maniac-fight
		"Procedure of fight with maniac. Do it in transaction."
		[]
		;; Get maniac in this room
		(def current-maniacs-ref (  :maniacs ((:name @*current-room* ) @rooms )  ) )
		;;(println (str (:name @*current-room*)) )
		;;(println @current-maniacs-ref)
		( if (not= @current-maniacs-ref #{})    										;; If some maniac in room 
			(if (@*current-emoji* @current-maniacs-ref)   ;; If here is maniac with same emoji  
							(println "Maniac don't kill you there" )
							( do 
										(kill-player)
										(println "YOU ARE DIED\nStart from the begining")
							) 
			)
		)
)

(defn gen-maniac []
		"Generagte maniac in random room"
  (get-temp-rooms)
  (def target-room (rand-nth temp-rooms))
  (def target-emotion (rand-nth (vec emoji) ) )
  ;;(println target-emotion)
  ;;(println target-room)
  ;;(println (target-room @rooms))

  (dosync 
  			( str  
  						"You gen maniac with emotion "
  						(alter ( :maniacs (target-room @rooms) ) conj  target-emotion 	)    ;; write maniac into room
  			;(say_loud "maniac is appear") 
  						"\r\n"
  			)
		)

)

(defn kill-maniac 
			"Kill maniac in room with emotion emotion"
			[room, emotion]
			(dosync
			
				(if ( (keyword emotion)  @( :maniacs ( (keyword room ) @rooms) ) ) ;; If maniac is in the room
								(do 
											(alter ( :maniacs ( (keyword room ) @rooms) ) disj  (keyword emotion) 	) ;; Remove maniac from room
											(println "Maniac died, uhahaha!")
								)
								( println "Maniac isn't here" )
					)
			)
)
