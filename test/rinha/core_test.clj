(ns rinha.core-test
  (:require [rinha.core :as sut]
            [io.pedestal.test :refer :all]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [clojure.test :refer :all]))

(defn test-request [verb url]
  (io.pedestal.test/response-for (::http/service-fn @sut/server) verb url))

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
    (is (= 404 (:status (test-request :get "/pessoas/xyz"))))))
