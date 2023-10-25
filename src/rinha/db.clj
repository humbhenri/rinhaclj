(ns rinha.db
  (:require [clojure.java.jdbc :as j]
            [clojure.string :as str]
            [honey.sql :as sql]
            [hikari-cp.core :as cp]
            [taoensso.timbre :as timbre]
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
                       (map str/lower-case (list (:apelido value) (:nome value))))
        id (UUID/randomUUID)]
    (try
      (->
       (j/insert! database-connection :pessoaentity (assoc value :stack stack :text text :id id))
       first)
      (catch java.lang.Exception e
        (timbre/error (.getMessage e))
        (if (str/includes? (.getMessage e) "ERROR: duplicate key value")
          (throw (ex-info "apelido duplicado" {:type :apelido-duplicado}))
          (throw (ex-info "erro genérico" {:type :erro-generico})))))))

(defn contagem-pessoas []
  (-> {:select [[[:count :*]]] :from [:pessoaentity]}
      sql/format
      select
      first
      :count))

(defn formata-pessoa [pessoa]
  (if pessoa
    (-> pessoa
        (update :stack #(str/split % #","))
        (dissoc :text))
    nil))



(defn pesquisa-termo [termo]
  (->> {:select [:*] :from :pessoaentity :where [:like :text (str/join ["%" termo "%"])]}
      sql/format
      select
      (map formata-pessoa)))

(defn detalhe-pessoa [id]
  (->> {:select [:*] :from :pessoaentity :where [:= :id (UUID/fromString id)]}
       sql/format
      select
      first
      formata-pessoa))

  ;; (cria-pessoa {:id (UUID/randomUUID) :apelido "humb3" :nome "Humberto" :nascimento "2000-10-01" :stack '("C#" "Node" "Oracle")})
;; (pesquisa-termo "humb")

;; (select (sql/format {:select [:*] :from :pessoaentity}))
;; (detalhe-pessoa (str (UUID/randomUUID)))
