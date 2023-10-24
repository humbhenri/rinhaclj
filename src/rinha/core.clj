(ns rinha.core (:require [io.pedestal.http :as http]
                   [io.pedestal.http.route :as route]
                   [io.pedestal.http.content-negotiation :as conneg]
                   [io.pedestal.interceptor.error :as error]
                   [clojure.string :as str]
                   [clojure.data.json :as json]
                   [rinha.db :as db]
                   [rinha.config :as config]))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok       (partial response 200))

(def created  (partial response 201))

(defn transform-content
  [body content-type]
  (case content-type
    "text/html"        body
    "text/plain"       body
    "application/edn"  (pr-str body)
    "application/json" (json/write-str body)))

(def supported-types ["application/json" "text/plain"])

(defn coerce-to
  [response content-type]
  (-> response
      (update :body transform-content content-type)
      (assoc-in [:headers "Content-Type"] content-type)))

(defn accepted-type
  [context]
  (get-in context [:request :accept :field] "text/plain"))

(def coerce-body
  {:name ::coerce-body
   :leave
   (fn [context]
     (if (get-in context [:response :headers "Content-Type"])
       context
       (update-in context [:response] coerce-to (accepted-type context))))})

(def content-neg-intc (conneg/negotiate-content supported-types))

(defn pesquisa-termo-route [request]
  (if-let [termo (get-in request [:query-params :t])]
    (ok (db/pesquisa-termo termo))
    {:status 400}))

(defn contagem-pessoas-route [request]
  (ok (str (db/contagem-pessoas)) {"content-type" "text/plain"}))

(def service-error-handler
  (error/error-dispatch [ctx ex]
                        :else
                        (assoc ctx :response {:status 400 :body "error"})))

(def routes
  (route/expand-routes
   #{
     ["/pessoas" :get [service-error-handler coerce-body content-neg-intc pesquisa-termo-route] :route-name :pesquisa-termo-route]
     ["/contagem-pessoas" :get contagem-pessoas-route :route-name :contagem-pessoas-route]}))

(def service-map
  {::http/routes routes
   ::http/type   :immutant
   ::http/port   (get-in (config/get-config) [:server :port])})

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
