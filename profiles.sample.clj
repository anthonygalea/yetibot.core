{:dev
 {:env
  {:yetibot-log-level "debug"

   ;; By default Yetibot runs an in-memory Datomic database, which will dissapear
   ;; on shutdown. Point it to your own running transactor if you'd like
   ;; persistent history and other features (e.g. aliases, statuses).
   :yetibot-db-datomic-url "datomic:mem://yetibot"

   ;; ADAPTERS

   ;; Yetibot can listen on multiple instances of each adapters type. Current
   ;; adapter types are Slack and IRC.
   ;;
   ;; :name is used as a uuid so it must be unique to this Yetibot. The uuid is
   ;; used by the API to post messages to the correct instance, and also used to
   ;; store room configuration for each adapter config. If you change it, all your
   ;; room configuration will be lost (unless you manually update the config with
   ;; the changed uuid).
   ;;
   ;; Each config map must have:
   ;; - a :type key with value :slack or :irc
   ;; - a :name key with a unique value (i.e. uuid)"
   ;;
   ;; Example configuring 3 adapters: 2 Slacks and 1 IRC:
   :yetibot-adapters-0-name "team-slack"
   :yetibot-adapters-0-type "slack"
   :yetibot-adapters-0-token "xoxb-111111111111111111111111111111111111"

   ;; :yetibot-adapters-1-name "kubernetes-slack"
   ;; :yetibot-adapters-1-type "slack"
   ;; :yetibot-adapters-1-token "xoxb-9999999999999999"

   ;; :yetibot-adapters-2-name "freenode-irc"
   ;; :yetibot-adapters-2-type "irc"
   ;; :yetibot-adapters-2-host "freenode.net"
   ;; :yetibot-adapters-2-port "9600"
   ;; :yetibot-adapters-2-username "yetibot"

   ;; Listens on port 3000 but this may be different for you if you (e.g. if you
   ;; use a load balancer or map ports in Docker).
   :yetibot-url "http://localhost:3000"

   ;;
   ;; WORK
   ;;

   :yetibot-github-token ""
   :yetibot-github-org-0 ""
   :yetibot-github-org-1 ""
   ;; :endpoint is optional: only specify if using GitHub enterprise.
   :yetibot-github-endpoint ""

   ;; `jira`
   :yetibot-jira-domain ""
   :yetibot-jira-user ""
   :yetibot-jira-password ""
   :yetibot-jira-projects-0-key "FOO"
   :yetibot-jira-projects-0-default-version-id "42"
   :yetibot-jira-default-issue-type-id "3"
   :yetibot-jira-sub-task-issue-type-id "27"
   :yetibot-jira-default-project-key "Optional"

   ;; s3
   :yetibot-s3-access-key ""
   :yetibot-s3-secret-key ""

   ;; send and receive emails with `mail`
   :yetibot-mail-host ""
   :yetibot-mail-user ""
   :yetibot-mail-pass ""
   :yetibot-mail-from ""
   :yetibot-mail-bcc ""

   ;;
   ;; FUN
   ;;

   ;;  `giphy`
   :yetibot-giphy-key ""

   ;; `meme`
   :yetibot-imgflip-username ""
   :yetibot-imgflip-password ""

   ;;
   ;; INFOs
   ;;

   :yetibot-ebay-appid ""

   ;; `twitter`: stream tweets from followers and followed topics directly into
   ;; chat, and post tweets
   :yetibot-twitter-consumer-key ""
   :yetibot-twitter-consumer-secret ""
   :yetibot-twitter-token ""
   :yetibot-twitter-secret ""
   ;; ISO 639-1 code: http://en.wikipedia.org/wiki/List-of-ISO-639-1-codes
   :yetibot-twitter-search-lang "en"

   ;; `image` - falback to bing if Google fails
   :yetibot-bing-search-key ""

   ;; `jen` - Jenkins
   ;; Multiple Jenkins instances can be configured. They can also be configured
   ;; at runtime, which writes to this config file. Only one `default-job` is
   ;; supported if multiple are specified, the first one wins.
   :yetibot-jenkins-cache-ttl "3600000"
   :yetibot-jenkins-instances-0-name ""
   :yetibot-jenkins-instances-0-uri ""
   :yetibot-jenkins-instances-0-default-job ""
   ;; If your Jenkins doesn't require auth, set user and api-key to some
   ;; non-blank value in order to pass the configuration check.
   :yetibot-jenkins-instances-0-user ""
   :yetibot-jenkins-instances-0-api-key ""

   ;; Set of Strings: Slack IDs or IRC users (which have ~ prefixes) of users who
   ;; can use the yetibot `eval` command.
   :yetibot-eval-priv-0 "U123123"
   :yetibot-eval-priv-1 "~awesomeperson"

   ;; Configure GitHub if you have your own fork of the yetibot repo. This will
   ;; allow opening feature requests on your fork.
   :yetibot-features-github-token ""
   :yetibot-features-github-user ""

   ;; SSH servers are specified in groups so that multiple servers which share
   ;; usernames and keys don't need to each specify duplicate config. Fill in
   ;; your own key-names below instead of `:server-a-host`. This is the short
   ;; name that the ssh command will refer to, e.g.: `ssh server-a-host ls -al`.
   :yetibot-ssh-groups-0-key "path-to-key"
   :yetibot-ssh-groups-0-user ""
   :yetibot-ssh-groups-0-servers-0-name ""
   :yetibot-ssh-groups-0-servers-0-host ""
   :yetibot-ssh-groups-0-servers-1-name ""
   :yetibot-ssh-groups-0-servers-1-host ""

   ;; `weather`
   :yetibot-weather-wunderground-key ""
   :yetibot-weather-wunderground-default-zip ""

   ;; `wolfram`
   :yetibot-wolfram-appid ""

   ;; `wordnik` dictionary
   :yetibot-wordnik-key ""

}}}
