(ns rinha.core (:require [io.pedestal.http :as http]
                   [io.pedestal.http.route :as route]
                   [io.pedestal.http.content-negotiation :as conneg]
                   [io.pedestal.interceptor.error :as error]
                   [io.pedestal.http.body-params :as body-params]
                   [clojure.string :as str]
                   [clojure.data.json :as json]
                   [taoensso.timbre :as timbre]
                   [rinha.db :as db]
                   [rinha.config :as config]))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok       (partial response 200))

(def created  (partial response 201))

(def bad-request  (partial response 400))

(def not-found (partial response 404))

(def unprocessable-content (partial response 422))

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
    (bad-request)))

(defn contagem-pessoas-route [request]
  (ok (str (db/contagem-pessoas)) {"content-type" "text/plain"}))

(defmacro if-let*
  ([bindings then]
   `(if-let* ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-let* ~(drop 2 bindings) ~then ~else)
        ~(if-not (second bindings) else))
     then)))

(defn detalhe-pessoa-route [request]
  (if-let* [id (get-in request [:path-params :id])
           detalhe (db/detalhe-pessoa id)]
    (ok detalhe)
    (not-found)))

(defn new-pessoa-route [request]
  (if-let* [pessoa (:json-params request)
            pessoa-criada (db/cria-pessoa pessoa)]
    (ok (:id pessoa-criada))
    (bad-request)))

(def service-error-handler
  (error/error-dispatch [ctx ex]
                        :else
                        (let [message (->> ex
                                           .getData
                                           :exception
                                           .getMessage)]
                          (timbre/error message)
                          (if (str/includes? message "ERROR: duplicate key value")
                            (assoc ctx :response (unprocessable-content "error"))
                            (assoc ctx :response (bad-request "error"))))))

(def routes
  (route/expand-routes
   #{
     ["/pessoas" :get [service-error-handler coerce-body content-neg-intc pesquisa-termo-route] :route-name :pesquisa-termo-route]
     ["/pessoas" :post [service-error-handler coerce-body content-neg-intc (body-params/body-params) new-pessoa-route] :route-name :new-pessoa-route]
     ["/pessoas/:id" :get [service-error-handler coerce-body content-neg-intc detalhe-pessoa-route] :route-name :detalhe-pessoa-route]
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
  (when @server
    (stop-dev))
  (start-dev))

(restart)
