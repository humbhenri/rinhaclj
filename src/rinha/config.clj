(ns rinha.config (:require [aero.core :refer [read-config]]
                           [clojure.java.io :as io]))

(defn get-config []
  (read-config (io/resource "config.edn")))
