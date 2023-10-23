(ns db
  (:require [clojure.java.jdbc :as j]
            [honey.sql :as sql])
  (:import [java.util UUID]))

(def pg-db {:dbtype "postgresql"
            :port 5432
            :dbname "mydatabase"
            :host (or (System/getenv "DB_HOST") "localhost")
            :user "sarah"
            :password "connor"})

(defn select [query]
  (j/query pg-db query))

(defn cria-pessoa [value]
  (j/insert! pg-db :pessoaentity value))

(defn contagem-pessoas []
  (-> {:select [[[:count :*]]] :from [:pessoaentity]}
      sql/format
      select
      first
      :count))

(cria-pessoa {:id (UUID/randomUUID) :apelido "joao"})

(contagem-pessoas)

(clojure.pprint/pprint (select (sql/format {:select [:*] :from :pessoaentity})))
