; ================================================= ;
; ========== try to code timer using java ========= ;
; ================================================= ;

(import java.util.TimerTask)
(import java.util.Timer)

(def ms 1000)

(defn prtimer [ms] 
    (println "work")
    (def timer (Timer. "Timer"))
    (if (> ms 0)
        (loop [stopTimer 6]
            (when (> stopTimer 0)
                (do
                    (println stopTimer)
                    (def timer (Timer. "Timer"))
                    (def task (proxy [TimerTask] [] (run [] (println "www"))))
                    (def timer-delay 1000)
                    (.schedule timer task timer-delay) 
                )
            (recur (dec stopTimer)))
        )
    )
)
(prtimer ms)
