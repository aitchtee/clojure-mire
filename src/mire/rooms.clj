(ns mire.rooms)

(def rooms (ref {}))

(defn load-room [rooms file]
  (let [room (read-string (slurp (.getAbsolutePath file)))]
    (conj rooms
          {(keyword (.getName file))
           {:name (keyword (.getName file))
            :desc (:desc room)
            :exits (ref (:exits room))
            :gold (ref (:gold room))
            :items (ref (or (:items room) #{}))
            :lock (ref (or (:lock room) #{}))
            :inhabitants (ref #{})
            :maniacs (ref #{})}})))

(defn testikitty
  []
  (def dict
    (for [room (vals (deref rooms))]
      (apply merge  {(keyword (room :name))  @(room :lock)})
  ))
  (def dic dict)
  (for [keke (vals dic)]
    ((def kek (keke :lock))
     (println kek)
      (conj kek "#{}")))
  (println dic)
)



(defn load-rooms
  "Given a dir, return a map with an entry corresponding to each file
  in it. Files should be maps containing room data."
  [rooms dir]
  (dosync
   (reduce load-room rooms
           (.listFiles (java.io.File. dir)))))

(defn add-rooms
  "Look through all the files in a dir for files describing rooms and add
  them to the mire.rooms/rooms map."
  [dir]
  (dosync
   (alter rooms load-rooms dir)))

(defn room-contains?
  [room thing]
  (@(:items room) (keyword thing)))

(defn room-containslock?
  [room lock1]
  (@(:lock room) (keyword lock1)))

(defn room-contains-gold?
  [room thing]
  (contains? @(:gold room) (keyword thing)))
