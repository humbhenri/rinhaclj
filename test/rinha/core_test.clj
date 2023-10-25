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

(defn cria-pessoa []
  (-> {
       :apelido (str "josé" (Math/random))
       :nome "José Roberto"
       :nascimento "2000-10-01"
       :stack ["C#", "Node", "Oracle"]} json/write-str))

(deftest cria-pessoa-test
  (testing "para criaçao de pessoa válida, deve retornar 201 com o header correto"
    (let [{:keys [status headers]} (test-post-request "/pessoas" (cria-pessoa))]
      (do
        (is (= 201 status))
        (is (str/includes? (get headers "Location") "/pessoas/"))))))

(run-tests 'rinha.core-test)
