(ns rinha.db
  (:require [clojure.java.jdbc :as j]
            [clojure.string :as str]
            [honey.sql :as sql]
            [hikari-cp.core :as cp]
            [rinha.config :as config])
  (:import [java.util UUID]))

(def cfg (config/get-config))

(def datasource-options
  {:username           (get-in cfg [:db :username])
   :password           (get-in cfg [:db :password])
   :port-number        (get-in cfg [:db :port])
   :database-name      (get-in cfg [:db :database])
   :server-name        (get-in cfg [:db :server])
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
  (let [stack (str/join "," (:stack value))
        text (str/join (->> (:stack value) (map str/lower-case) str/join)
                       (list (:apelido value) (:nome value)))]
    (j/insert! database-connection :pessoaentity (assoc value :stack stack :text text))))

(defn contagem-pessoas []
  (-> {:select [[[:count :*]]] :from [:pessoaentity]}
      sql/format
      select
      first
      :count))

(defn formata-pessoa [pessoa]
  (-> pessoa
      (update :stack #(str/split % #","))
      (dissoc :text)))

(defn pesquisa-termo [termo]
  (->> {:select [:*] :from :pessoaentity :where [:like :text (str/join ["%" termo "%"])]}
      sql/format
      select
      (map formata-pessoa)))

;; (cria-pessoa {:id (UUID/randomUUID) :apelido "humb" :nome "Humberto" :nascimento "2000-10-01" :stack '("C#" "Node" "Oracle")})
;; (pesquisa-termo "humb")
