(ns mire.work)

 (def work (ref {}))

 (defn work "This is 'yes/no' work. You have to answer 'yes' or 'no'.\r\n"
     [money, questions]
 (defn answer
     (str (first (read-line)))
 )
 (case answer
     "yes" : (println "Let's start!") (println prompt)
     "no"  : (println "Ok, maybe next time.") (println prompt)
 )

 )

(def answer (read-line)
(if(= answer "yes")"Let's start" "Ok, maybe next time")
)
(println (str "You typed \"" answer "\""))


