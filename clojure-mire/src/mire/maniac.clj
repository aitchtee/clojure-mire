(ns mire.maniac
  (:use [mire.rooms :only [rooms]]))

(def maniacs (ref {}))
(def temp-rooms (ref {}))


(defn get-temp-rooms []

  (def res
    (for [room (vals (deref rooms))]
      (do
        (apply merge {:name (room :name)})
      )
    )
  )
  (filter (fn [x] (do
                      (not= x nil)
                      (not= (x :name) (keyword "start"))
                    )
            ) res)
  (def temp-rooms res)
)


(defn gen-maniac []
  (get-temp-rooms)
  ;;(def target-maniac (rand-nth temp-rooms))
  (println temp-rooms)
  ;;(println target-maniac)
  ;;(def res
  ;;  (for [room temp-rooms]
  ;;    (do
  ;;      (if (= (room :name) (target-maniac :name))
  ;;        (apply merge {:name (room :name)})
  ;;        (apply merge maniacs)
  ;;      )
  ;;    )
  ;;  )
  ;;)
  ;;(def maniacs res)
  ;;(println maniacs)
)
