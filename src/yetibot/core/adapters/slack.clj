(ns yetibot.core.adapters.slack
  (:require
    [clojure.core.memoize :as memo]
    [yetibot.core.adapters.adapter :as a]
    [robert.bruce :refer [try-try-again] :as rb]
    [gniazdo.core :as ws]
    [clojure.string :as s]
    [schema.core :as sch]
    [yetibot.core.interpreter :refer [*chat-source*]]
    [yetibot.core.models.users :as users]
    [yetibot.core.util.http :refer [html-decode]]
    [clj-slack
     [users :as slack-users]
     [chat :as slack-chat]
     [channels :as channels]
     [groups :as groups]
     [rtm :as rtm]]
    [slack-rtm.core :as slack]
    [taoensso.timbre :as log :refer [info warn error]]
    [yetibot.core.config-mutable :refer [update-config get-config 
                                         apply-config]]
    [yetibot.core.handler :refer [handle-raw]]
    [yetibot.core.chat :refer [base-chat-source chat-source
                               chat-data-structure *target* *adapter*]]
    [yetibot.core.util :as utl]))

(def channel-cache-ttl 60000)

(defn slack-config
  "Transforms yetibot config to expected Slack config"
  [config]
  {:api-url (or (:endpoint config) "https://slack.com/api")
   :token (:token config)})

(defn list-channels [config] (channels/list (slack-config config)))

(def channels-cached
  (memo/ttl
    (comp :channels list-channels)
    :ttl/threshold channel-cache-ttl))

(defn channel-by-id [id config]
  (first (filter #(= id (:id %)) (channels-cached config))))


(defn list-groups [config] (groups/list (slack-config config)))

(defn channels-in
  "all channels that yetibot is a member of"
  [config]
  (filter :is_member (:channels (list-channels config))))

(defn self
  "Slack acount for yetibot from `rtm.start` (represented at (-> @conn :start)).
   You must call `start` in order to define `conn`."
  [conn]
  (-> @conn :start :self))

;;;;

(defn chan-or-group-name
  "Takes either a channel or a group and returns its name as a String.
   If it's a public channel, # is prepended.
   If it's a group, just return the name."
  [chan-or-grp]
  (str (when (:is_channel chan-or-grp) "#")
       (:name chan-or-grp)))

(defn rooms
  "A vector of channels and any private groups by name"
  [config]
  (concat
    (map :name (:groups (list-groups config)))
    (map #(str "#" (:name %)) (channels-in config))))

(defn send-msg [config msg]
  (slack-chat/post-message
    (slack-config config) *target* msg
    {:unfurl_media "true" :as_user "true"}))

(defn send-paste [config msg]
  (slack-chat/post-message
    (slack-config config) *target* ""
    {:unfurl_media "true" :as_user "true"
     :attachments [{:pretext "" :text msg}]}))

;; formatting

(defn unencode-message
  "Slack gets fancy with URL detection, channel names, user mentions, as
   described in https://api.slack.com/docs/formatting. This can break support
   for things where YB is expecting a URL (e.g. configuring Jenkins), so strip
   it for now. Replaces <X|Y> with Y."
  [body]
  (-> body
    (s/replace #"\<(.+)\|(.+)\>" "$2")
    html-decode))


(defn entity-with-name-by-id
  "Takes a message event and translates a channel ID, group ID, or user id from
   a direct message (e.g. 'C12312', 'G123123', 'D123123') into a [name entity]
   pair. Channels have a # prefix"
  [config event]
  (let [sc (slack-config config)
        chan-id (:channel event)]
    (condp = (first chan-id)
      ;; direct message - lookup the user
      \D (let [e (:user (slack-users/info sc (:user event)))]
           [(:name e) e])
      ;; channel
      \C (let [e (:channel (channels/info sc chan-id))]
           [(str "#" (:name e)) e])
      ;; group
      \G (let [e (:group (groups/info sc chan-id))]
           [(:name e) e])
      (throw (ex-info "unknown entity type" event)))))



;; events

(defn on-channel-join [e]
  (log/info "channel join" e)
  (let [cs (chat-source (:channel e))
        user-model (users/get-user cs (:user e))]
    (handle-raw cs user-model :enter nil)))

(defn on-channel-leave [e]
  (log/info "channel leave" e)
  (let [cs (chat-source (:channel e))
        user-model (users/get-user cs (:user e))]
    (handle-raw cs user-model :leave nil)))

(defn on-message-changed [{:keys [channel] {:keys [user text]} :message} config]
  (log/info "message changed")
  (let [[chan-name entity] (entity-with-name-by-id config {:channel channel
                                                           :user user})
        cs (chat-source chan-name)
        user-model (users/get-user cs user)]
    (binding [*target* channel]
      (handle-raw cs
                  user-model
                  :message
                  (unencode-message text)))))

(defn on-message [conn config event]
  (log/info "message" event)
  (log/info "platform-name" (a/platform-name *adapter*))
  (if-let [subtype (:subtype event)]
    ; handle the subtype
    (condp = subtype
      "channel_join" (on-channel-join event)
      "channel_leave" (on-channel-leave event)
      "message_changed" (on-message-changed event config)
      ; do nothing if we don't understand
      nil)
    ; don't listen to yetibot's own messages
    (when (not= (:id (self conn)) (:user event))
      (let [chan-id (:channel event)
            [chan-name entity] (entity-with-name-by-id config event)
            cs (chat-source chan-name)
            user-model (users/get-user cs (:user event))]
        (binding [*target* chan-id]
          (handle-raw cs
                      user-model
                      :message
                      (unencode-message (:text event))))))))

(defn on-hello [event] (log/info "hello" event))

(defn on-connect [e] (log/info "connect" e))

(declare restart)

(defn on-close [conn config status]
  (log/info "close" (:name config) status)
  (when (not= (:reason status) "Shutdown")
    (try-try-again
      {:decay 1.1 :sleep 5000 :tries 500}
      (fn []
        (log/info "attempt no. " rb/*try* " to rereconnect to slack")
        (when rb/*error* (log/info "previous attempt errored:" rb/*error*))
        (when rb/*last-try* (log/warn "this is the last attempt"))
        (restart conn config)))))

(defn on-error [exception]
  (log/error "error" exception))

(defn handle-presence-change [e]
  (let [active? (= "active" (:presence e))
        id (:user e)
        source (select-keys (base-chat-source) [:adapter])]
    #_(log/trace id "presence change active?=" active?)
    (users/update-user source id {:active? active?})))

(defn on-presence-change [e]
  (handle-presence-change e))

(defn on-manual-presence-change [e]
  (log/debug "manual presence changed" e)
  (handle-presence-change e))

(defn room-persist-edn
  "Update config.edn when a room is joined or left"
  [uuid room joined?]
  (let
    [adapters (get-config sch/Any [:yetibot :adapters])
     instance-index (first (utl/indices #(= (:name %) uuid) adapters))
     adapter (nth adapters instance-index)
     exists? (contains? adapter :rooms)]

    (cond
      joined? (if exists?
                (apply-config
                  [:yetibot :adapters instance-index :rooms]
                  (fn [x] (conj x room)))
                (apply-config
                  [:yetibot :adapters instance-index]
                  (fn [x] (assoc x :rooms #{room}))))

      :left (when exists?
              (apply-config
                [:yetibot :adapters instance-index :rooms]
                (fn [x] (disj x room)))))))

(defn on-channel-joined
  "Fires when yetibot gets invited and joins a channel or group"
  [e]
  (log/debug "channel joined" e)
  (let [c (:channel e)
        {:keys [uuid room] :as cs} (chat-source (:id c))
        user-ids (:members c)]
    (room-persist-edn uuid room true)
    (log/debug "adding chat source" cs "for users" user-ids)
    (dorun (map #(users/add-chat-source-to-user cs %) user-ids))))

(defn on-channel-left
  "Fires when yetibot gets kicked from a channel or group"
  [e]
  (log/debug "channel left" e)
  (let [c (:channel e)
        {:keys [uuid room] :as cs} (chat-source c)
        users-in-chan (users/get-users cs)]
    (room-persist-edn uuid room false)
    (log/debug "remove users from" cs (map :id users-in-chan))
    (dorun (map (fn [u] (users/remove-user cs (:id u))) users-in-chan))))

;; users

(defn filter-chans-or-grps-containing-user [user-id chans-or-grps]
  (filter #((-> % :members set) user-id) chans-or-grps))

(defn reset-users-from-conn [conn]
  (let [groups (-> @conn :start :groups)
        channels (-> @conn :start :channels)
        users (-> @conn :start :users)]
    (dorun
      (map
        (fn [{:keys [id] :as user}]
          (let [filter-for-user (partial filter-chans-or-grps-containing-user id)
                ; determine which channels and groups the user is in
                chans-or-grps-for-user (concat (filter-for-user channels)
                                               (filter-for-user groups))
                active? (= "active" (:presence user))
                ; turn the list of chans-or-grps-for-user into a list of chat sources
                chat-sources (set (map (comp chat-source chan-or-group-name) chans-or-grps-for-user))
                ; create a user model
                user-model (users/create-user (:name user) active? (assoc user :mention-name (str "<@" (:id user) ">")))]
            (if (empty? chat-sources)
              (users/add-user-without-room (:adapter (base-chat-source)) user-model)
              (dorun
                ; for each chat source add a user individually
                (map (fn [cs] (users/add-user cs user-model)) chat-sources)))))
        users))))

;; lifecycle

(defn stop [conn]
  (when @conn
    (log/info "Closing" @conn)
    (slack/send-event (:dispatcher @conn) :close))
  (reset! conn nil))

(defn restart
  "conn is a reference to an atom.
   config is a map"
  [conn config]
  (reset! conn (slack/connect (slack-config config)
                              :on-connect on-connect
                              :on-error on-error
                              :on-close (partial on-close conn config)
                              :presence_change on-presence-change
                              :channel_joined on-channel-joined
                              :group_joined on-channel-joined
                              :channel_left on-channel-left
                              :group_left on-channel-left
                              :manual_presence_change on-manual-presence-change
                              :message (partial on-message conn config)
                              :hello on-hello))
  (reset-users-from-conn conn))

(defn start [adapter conn config]
  (stop conn)
  (binding [*adapter* adapter]
    (info "adapter" adapter "starting up with config" config)
    (restart conn config)))

;; adapter impl

(defrecord Slack [config config-idx conn]
  a/Adapter

  (a/uuid [_] (:name config))

  (a/platform-name [_] "Slack")

  (a/rooms [_] (rooms config))

  (a/send-paste [_ msg] (send-paste config msg))

  (a/send-msg [_ msg] (send-msg config msg))

  (a/join [_ room] (str "Slack bots such as myself can't join rooms on their own. Use /invite @yetibot from the channel you'd like me to join instead. ✌️"))

  (a/leave [_ room] (str "Slack bots such as myself can't leave rooms on their own. Use /kick @yetibot from the channel you'd like me to leave instead. 👊"))

  (a/chat-source [_ room] (chat-source room))

  (a/stop [_] (stop conn))

  (a/start [adapter] (start adapter conn config)))

(defn make-slack
  [idx config]
  (->Slack config idx (atom nil)))
