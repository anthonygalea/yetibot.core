(ns yetibot.core.adapters.campfire
  "NOTE: Campfire is no longer supported!
   This code is currently left in place in case anyone still uses Campfire and
   wants Yetibot to support it. (If so, submit a PR or else ping me!)"
  (:require
    [schema.core :as schema]
    [taoensso.timbre :refer [info warn error]]
    [yetibot.core.chat :as chat :as chat]
    [yetibot.core.handler :refer [handle-raw]]
    [http.async.client :as c]
    [clojure.data.json :as json]
    [yetibot.core.models.users :as users]
    [clojure.string :as s]
    [clojure.stacktrace :as st]
    [yetibot.core.config :refer [get-config conf-valid?]]
    [yetibot.core.util.http :as http]
    [yetibot.core.util.format :as fmt]
    [clj-campfire.core :as cf]))

; Settings

(comment

  (def config (get-config schema/Any [:yetibot :adapters :campfire]))

  (def room-id (:room-id config))

  (def chat-source [:campfire room-id])

  (def cf-settings
    {:api-token (:api-key config),
     :ssl true,
     :sub-domain (:subdomain config)})
  (def room (:room config))
  (def json-headers {:content-type "application/json"})
  (def escapees {\" "\\\""})

  (def base-uri (str "https://" (:sub-domain cf-settings) ".campfirenow.com"))
  (def streaming-uri "https://streaming.campfirenow.com")
  (def auth {:user (:api-key config) :password "x" :preemptive true})

  (defn send-message [msg] (cf/message cf-settings room msg))

  (defn send-paste [p] (cf/paste cf-settings room p))

  (defn send-tweet [t] (cf/tweet cf-settings room t))

  (def send-to-all send-message)

  (defn self [token]
    (let [auth {:user token :password "x"}
          uri (str base-uri "/users/me.json")]
      (http/get-json uri auth)))

  (defn play-sound [sound] (cf/play-sound cf-settings room sound))

  (defn join-room []
    (with-open [client (c/create-client)]
      (let [uri (str base-uri "/room/" room-id "/join.json")
            resp (c/POST client uri :auth auth)]
        (println (str "✓ Joining room at " uri))
        (c/await resp))))

  (defn get-room []
    (let [uri (str base-uri "/room/" room-id ".json")]
      (http/get-json uri auth)))

  ; Monitor the chat room with the Streaming API
  (defn listen-to-chat [message-callback]
    (with-open [client (c/create-client)]
      (join-room)
      (let [uri (str streaming-uri "/room/" room-id "/live.json")
                resp (c/stream-seq client :get uri :auth auth :timeout -1)]
        (println (str "✓ Start listening on streaming API in room " room))
        (doseq [s (c/string resp)]
          (if (not (empty? (s/trim (str s))))
            ; Campfire sometimes returns multiple lines of json objects at a
            ; time, 1 per line so split the lines before parsing json
            (doseq [line (s/split s #"\cM")]
              (future
                (try
                  (let [json (json/read-json line)]
                    (println json)
                    (message-callback json))
                  (catch Exception ex
                    (println (str "Exception in chat handler " ex))
                    (st/print-stack-trace (st/root-cause ex) 12))))))))))

  (defn reset-users-from-room [room]
    (let [users (-> room :room :users)]
      (dorun
        (map
          (fn [{:keys [id name] :as user-info}]
            (users/add-user chat-source (users/create-user name user-info)))
          users))))

  (defn reset-users [] (reset-users-from-room (get-room)))

  (def messaging-fns
    {:msg send-message
     :paste send-paste})

  (defn handle-enter [json]
    (handle-raw
      chat-source
      (users/create-user (:name json) json)
      :enter
      nil))

  (defn handle-leave [json]
    (handle-raw
      chat-source
      (users/create-user (:name json) json)
      :leave
      nil))

  (defn handle-text-message [json]
    "Parse a `TextMessage` campfire event into a command and its args"
    (let [user (users/get-user chat-source (:user_id json))]
      ; (binding [
      ;           ; chat/*messaging-fns* messaging-fns
      ;           ]
        (handle-raw chat-source user :message (:body json))))

  (defn handle-campfire-event [json]
    (let [event-type (:type json)]
      (condp = event-type ; Handle the various types of messages
        "TextMessage" (handle-text-message json)
        "PasteMessage" (handle-text-message json)
        (println "No handler for " event-type))))

  (defn start []
    (if (conf-valid? config)
      (do
        ; (register-chat-adapter 'yetibot.core.adapters.campfire)
        (future (reset-users))
        (future
          (while true
            (try
              (listen-to-chat handle-campfire-event)
              (catch Exception ex
                (error "Exception while listening to streaming api" (str ex))))
            (error "Something bad happened. Sleeping for 2 seconds before reconnect")
            (. Thread (sleep 2000)))))
      (info "✗ Campfire is not configured"))))
