(ns mire.maniac
  (:use [mire.rooms :only [rooms]])
  (:use [mire.emojiList]
  						[mire.player])
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

;;Maniac functions
(defn maniac-fight
		"Procedure of fight with maniac. Do it in transaction."
		[]
		;; Get maniac in this room
		(def current-maniacs-ref (  :maniacs ((:name @*current-room* ) @rooms )  ) )
		;(println @current-maniacs-ref)
		(if (@*current-emoji* @current-maniacs-ref)   ;; If here is maniac with same emoji  
						(println "Maniac don't kill you there" )
						(println "YOU ARE DIED") 
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
  			(alter ( :maniacs (target-room @rooms) ) conj  target-emotion 	)   ;; write maniac into room
  			;(say_loud "maniac is appear") 
		)

)

(defn kill-maniac 
			"Kill maniac in room with emotion emotion"
			[room, emotion]
			(dosync
			
				(if ( (keyword emotion)  @( :maniacs ( (keyword room ) @rooms) ) ) ;; If maniac is in the room
								(do 
											(alter ( :maniacs ( (keyword room ) @rooms) ) disj  (keyword emotion) 	) ;; Remove maniac from room
											(str "Maniac died, uhahaha!")
								)
								( str "Maniac isn't here" )
					)
			)
)