(ns yetibot.core.logging
  (:require
    [schema.core :as s]
    [yetibot.core.config :refer [get-config]]
    [yetibot.core.util :refer [with-fresh-db]]
    [yetibot.core.db.log :as log]
    [taoensso.timbre.appenders.3rd-party.rolling :refer [rolling-appender]]
    [taoensso.timbre.appenders.core :refer [println-appender]]
    [taoensso.timbre
     :as timbre
     :refer [trace debug info warn error fatal spy with-log-level]]))

(def config-schema String)

(defn log-level []
  (let [v (get-config config-schema [:yetibot :log :level])]
    (if (:error v)
      ;; default config level
      :warn
      (keyword (:value v)))))

(defn rolling-appender-enabled?
  "Rolling appender is enabled by default. Disable it by setting enabled=false"
  []
  (let [v (get-config String [:yetibot :log :rolling :enabled])]
    (if-let [enabled (:value v)]
      (not= enabled "false")
      true)))

(defn start []
  (if (rolling-appender-enabled?)
    (timbre/set-config!
      {:level (log-level)
       :appenders
       ;; stdout
       {:println (println-appender {:stream :auto})
        ;; rolling log files
        :rolling-appender (rolling-appender 
                            {:path "/var/log/yetibot/yetibot.log"
                             :pattern :daily})}})))
