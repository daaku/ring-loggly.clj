(ns ring-loggly.core
  "Pipe Java Logging to Loggly in a single background thread."
  {:author "Naitik Shah"}
  (:require [http.async.client :as c]))

(def levels (array-map :trace (.intValue java.util.logging.Level/FINEST)
                       :debug (.intValue java.util.logging.Level/FINE)
                       :info  (.intValue java.util.logging.Level/INFO)
                       :warn  (.intValue java.util.logging.Level/WARNING)
                       :error (.intValue java.util.logging.Level/SEVERE)
                       :fatal (.intValue java.util.logging.Level/SEVERE)))
(def ^{:dynamic true} *global-level* :info)
(def ^{:dynamic true} *named-levels* {})

(defn set-global-level! [level]
  (alter-var-root #'*global-level* (constantly level)))

(defn set-named-level! [name level]
  (alter-var-root #'*named-levels* assoc name level))

(defn- log-record? [record]
  (>= (.intValue (.getLevel record))
      (levels (or (*named-levels* (.getLoggerName record)) *global-level*))))

(defn- send-one [client input-url record]
  (c/POST client input-url
          :headers {"Content-Type" "text/plain"}
          :body (.format (java.util.logging.SimpleFormatter.) record)))

(defn- send-all [input-url logs]
  (with-open [client (c/create-client)]
    (dorun (map c/await (doall (map #(send-one client input-url %) logs))))))

(gen-class
  :name ring-loggly.core.handler
  :extends java.util.logging.Handler
  :state state
  :init init
  :constructors {[String int] []}
  :prefix "handler-")

(defn- handler-init [input-url interval]
  (let [thread-active (atom true)
        pending-logs (atom [])]
    (future (while @thread-active
              (Thread/sleep interval)
              (let [logs @pending-logs]
                (if (and (not (empty? logs))
                         (compare-and-set! pending-logs logs []))
                  (send-all input-url logs)))))
    [[] {:thread-active thread-active :pending-logs pending-logs}]))

(defn- handler-publish [this record]
  (if (log-record? record)
    (swap! (:pending-logs (.state this)) conj record)))

(defn- handler-flush [this])

(defn- handler-close [this]
  (reset! (:thread-active (.state this)) false))

(defn wrap-loggly [input-url interval app]
  (.reset (java.util.logging.LogManager/getLogManager))
  (set-global-level! :info)
  (set-named-level!
    "com.ning.http.client.providers.netty.NettyAsyncHttpProvider" :warn)
  (.addHandler (java.util.logging.Logger/getLogger "")
               (ring-loggly.core.handler. input-url interval)))
