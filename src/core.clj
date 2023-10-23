(ns core (:require [io.pedestal.http :as http]
                   [io.pedestal.http.route :as route]
                   [io.pedestal.http.content-negotiation :as conneg]
                   [clojure.string :as str]
                   [clojure.data.json :as json]))

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

(def routes
  (route/expand-routes
   #{
     ["/pessoas" :get pessoas :route-name :pessoas]}))

(def service-map
  {::http/routes routes
   ::http/type   :immutant
   ::http/port   (parse-long (or (System/getenv "PORT") "3000"))})

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
