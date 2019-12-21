(ns net.cgrand.parsley.conjdemo
  (:require [net.cgrand.parsley :as p])
  (:use [clojure.pprint :only [pprint]]))

(def sexpr
  (p/parser {:main :expr*, 
             :root-tag :root, 
             :space [#"\s+" :?]}
    :expr #{:list :symbol} 
    :symbol #"[a-zA-Z-]+"
    :list ["(" :expr* ")"]))

(defn terse [t]
  (if (map? t)
    (into [(:tag t)] (map terse (:content t)))
    t))

(def tp (comp pprint terse))

(def buf (p/incremental-buffer sexpr))

(p/parse-tree buf)

(tp *1)

(defn rand-tree [syms n]
  (if (zero? n)
    (rand-nth syms)
    (repeatedly (rand-int 8) #(rand-tree syms (dec n)))))

(def input (with-out-str 
                (pprint (rand-tree ['hello 'conj] 5))))

(println input)

(def buf2 (time 
            (-> buf 
              (p/edit 0 0 input))))

(def pt2 (time (p/parse-tree buf2)))

(def buf3 (time (-> buf2 
                  (p/edit 13 1 "s"))))

(def pt3 (time (p/parse-tree buf3)))

(println "-------------------")
(tp pt3)


(def buf4 (time (-> buf3 (p/edit 0 0 "("))))

(def pt4 (time (p/parse-tree buf4)))

(println "-------------------")
(tp pt4)





