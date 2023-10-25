(ns rinha.core-test
  (:require [rinha.core :as sut]
            [io.pedestal.test :refer :all]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [clojure.test :refer :all]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]
            [clojure.data.json :as json]))

(defn test-request [verb url]
  (response-for (::http/service-fn @sut/server) verb url))

(defn test-post-request [url body]
  (response-for (::http/service-fn @sut/server)
                :post url
                :headers {"Content-Type" "application/json"}
                :body body))

(def service
  "Service under test"
  (:io.pedestal.http/service-fn (io.pedestal.http/create-servlet sut/service-map)))

; Create the test url generator
(def url-for
  "Test url generator."
  (route/url-for-routes sut/routes))

(deftest pesquisa
  (testing "pesquisa pessoas"
    (testing "/pessoas sem termo de busca deve retornar erro com código 400"
      (is (= 400 (:status (test-request :get "/pessoas")))))
    (testing "com termo de busca deve ter status 200"
      (is (= 200 (:status (test-request :get "/pessoas?t=node")))))))

(deftest detalhe
  (testing "detalhe de uma pessoa que nao existe deve retornar 404"
    (is (= 404 (:status (test-request :get (str "/pessoas/" (java.util.UUID/randomUUID))))))))

(defn cria-pessoa [& {:keys [apelido nome nascimento stack] :or
                      {apelido (str "jose" (Math/random)),
                       nome "José",
                       nascimento "2000-01-01",
                       stack ["C#", "Node", "Oracle"]}}]
  (-> {
       :apelido apelido
       :nome nome
       :nascimento nascimento
       :stack stack} json/write-str))

(deftest cria-pessoa-test
  (testing "para criaçao de pessoa válida, deve retornar 201 com o header correto"
    (let [{:keys [status headers]} (test-post-request "/pessoas" (cria-pessoa))]
      (is (= 201 status))
      (is (str/includes? (get headers "Location") "/pessoas/"))))
  (testing "stack é opcional"
    (let [{:keys [status headers]} (test-post-request "/pessoas" (cria-pessoa :stack nil))]
      (is (= 201 status))
      (is (str/includes? (get headers "Location") "/pessoas/"))))
  (testing "apelido deve ser único"
    (let [apelido (str "teste" (Math/random))]
      (test-post-request "/pessoas" (cria-pessoa :apelido apelido))
      (let [{:keys [status headers]} (test-post-request "/pessoas" (cria-pessoa :apelido apelido))]
        (is (= 422 status)))))
  (testing "nome é obrigatório"
    (let [{:keys [status headers]} (test-post-request "/pessoas" (cria-pessoa :nome nil))]
      (is (= 422 status))))
  (testing "apelido é obrigatório"
    (let [{:keys [status headers]} (test-post-request "/pessoas" (cria-pessoa :apelido nil))]
      (is (= 422 status))))
  (testing "nome deve ter no máximo tamanho 100"
    (let [{:keys [status headers]} (test-post-request "/pessoas" (cria-pessoa :nome (apply str (repeat 101 "1"))))]
      (is (= 422 status))))
  (testing "apelido deve ter no máximo tamanho 32"
    (let [{:keys [status headers]} (test-post-request "/pessoas" (cria-pessoa :apelido (apply str (repeat 33 "1"))))]
      (is (= 422 status))))
  (testing "nascimento deve ter formato de data"
    (let [{:keys [status headers]} (test-post-request "/pessoas" (cria-pessoa :nascimento "ksjfklsjdflkjs"))]
      (is (= 422 status))))
  (testing "cada elemento de stack deve ter no máximo tamanho 32"
    (let [{:keys [status headers]} (test-post-request "/pessoas" (cria-pessoa :stack [(apply str (repeat 33 "1"))]))]
      (is (= 422 status)))))

(run-tests 'rinha.core-test)
