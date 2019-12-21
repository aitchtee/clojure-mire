(defproject mire "0.13.1"
  :description "A multiuser text adventure game/learning project."
  :main ^:skip-aot mire.server
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [server-socket "1.0.0"]
                 [org.immutant/web "2.0.0-beta2"]
                 [compojure "1.1.8"]
                 [ring/ring-core "1.3.0"]
                 [environ "1.0.0"]
                 [clj-tcp "1.0.1"]
                 ])
