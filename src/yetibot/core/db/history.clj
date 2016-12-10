(ns yetibot.core.db.history
  (:refer-clojure :exclude [update])
  (:require [datomico.core :as dc]
            [datomico.action :refer [all where raw-where]]))


;;;; schema

(def model-namespace :history)

(def schema (dc/build-schema model-namespace
                             [[:chat-source-adapter :keyword]
                              [:chat-source-room :string]
                              [:user-id :string]
                              [:user-name :string]
                              [:body :string]]))

(dc/create-model-fns model-namespace)
