(ns yetibot.core.db.alias
  (:refer-clojure :exclude [update])
  (:require [datomico.core :as dc]
            [datomico.db :refer [q]]
            [datomico.action :refer [all where raw-where]]))

(def model-ns :alias)

(def schema (dc/build-schema model-ns
                             [[:userid :string] ; user-id was a long and can't be changed
                              [:cmd-name :string]
                              [:cmd :string]
                              [:alias-cmd :string]]))

(dc/create-model-fns model-ns)
