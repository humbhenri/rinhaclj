(ns core-test
  (:require [core :as sut]
            [io.pedestal.test :refer :all]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [clojure.test :refer :all]))

(defn test-request [verb url]
  (io.pedestal.test/response-for (::http/service-fn @core/server) verb url))

(def service
  "Service under test"
  (:io.pedestal.http/service-fn (io.pedestal.http/create-servlet sut/service-map)))

; Create the test url generator
(def url-for
  "Test url generator."
  (route/url-for-routes (route/expand-routes sut/routes)))


(deftest rinha-test
  (testing "pesquisa pessoas"
    (testing "/pessoas sem termo de busca deve retornar erro com código 400"
      (is (= 400 (:status (test-request :get "/pessoas")))))))
