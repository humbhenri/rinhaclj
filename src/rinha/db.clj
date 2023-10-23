(ns rinha.db
  (:require [clojure.java.jdbc :as j]
            [honey.sql :as sql]
            [hikari-cp.core :as cp])
  (:import [java.util UUID]))


(def datasource-options
  {:username           "sarah"
   :password           "connor"
   :port-number        5432
   :database-name      "mydatabase"
   :server-name        (or (System/getenv "DB_HOST") "localhost")
   :auto-commit        true
   :read-only          false
   :adapter            "postgresql"
   :connection-timeout 30000
   :validation-timeout 5000
   :idle-timeout       600000
   :max-lifetime       1800000
   :minimum-idle       10
   :maximum-pool-size  20
   :pool-name          "db-pool"
   :register-mbeans    false})

(defonce datasource
  (delay (cp/make-datasource datasource-options)))

(def database-connection {:datasource @datasource})

(defn select [query]
  (j/query database-connection query))

(defn cria-pessoa [value]
  (j/insert! database-connection :pessoaentity value))

(defn contagem-pessoas []
  (-> {:select [[[:count :*]]] :from [:pessoaentity]}
      sql/format
      select
      first
      :count))

;; (cria-pessoa {:id (UUID/randomUUID) :apelido "joao"})

(contagem-pessoas)

;; (select (sql/format {:select [:*] :from :pessoaentity}))
