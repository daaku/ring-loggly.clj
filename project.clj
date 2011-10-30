(defproject ring-loggly "1.0.0"
  :description "Pipe Java Logging to Loggly in a backaground thread."
  :aot [ring-loggly.core]
  :dependencies
    [[org.clojure/clojure "1.3.0"]
     [org.clojure/tools.logging "0.2.3"]
     [http.async.client "0.3.1"]
     [org.slf4j/slf4j-jdk14 "1.6.3"]]
  :dev-dependencies
    [[org.clojure/clojure "1.3.0"]
     [vimclojure/server "2.3.0-SNAPSHOT"]])
