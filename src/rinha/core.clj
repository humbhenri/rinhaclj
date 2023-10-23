(ns rinha.core (:require [io.pedestal.http :as http]
                   [io.pedestal.http.route :as route]
                   [io.pedestal.http.content-negotiation :as conneg]
                   [clojure.string :as str]
                   [clojure.data.json :as json]
                   [clojure.java.io :as io]
                   [rinha.db :as db]
                   [aero.core :refer [read-config]]))

(defn get-config []
  (read-config (io/resource "config.edn")))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok       (partial response 200))

(def created  (partial response 201))

(def echo
  {:name :echo
   :enter
   (fn [context]
     (let [request (:request context)
           response (ok context)]
       (assoc context :response response)))})

(def supported-types ["application/json" "text/plain"])

(def content-neg-intc (conneg/negotiate-content supported-types))

(defn pessoas [request]
  (if-let [termo (get-in request [:query-params :t])]
    {:status 200}
    {:status 400}))

(defn contagem-pessoas-route [request]
  (ok (str (db/contagem-pessoas)) {"content-type" "text/plain"}))

(def routes
  (route/expand-routes
   #{
     ["/pessoas" :get pessoas :route-name :pessoas]
     ["/contagem-pessoas" :get contagem-pessoas-route :route-name :contagem-pessoas-route]}))

(def service-map
  {::http/routes routes
   ::http/type   :immutant
   ::http/port   (get-in (get-config) [:server :port])})

(defn start []
  (http/start (http/create-server service-map)))

;; For interactive development
(defonce server (atom nil))

(defn start-dev []
  (reset! server
          (http/start (http/create-server
                       (assoc service-map
                              ::http/join? false)))))

(defn stop-dev []
  (http/stop @server))

(defn restart []
  (stop-dev)
  (start-dev))

