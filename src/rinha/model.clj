(ns rinha.model
  (:require [clova.core :refer :all]))

(defn validate-stack [stack]
  (or (nil? stack)
      (empty? stack)
      (every? #(and (< (count %) 33) (> (count %) 0)) stack)))

(def spec [:nome required? stringy? [longer? 0] [shorter? 101]
           :apelido required? stringy? [longer? 0] [shorter? 33]
           :nascimento required? [matches? #"\d{4}-\d{2}-\d{2}"]
           :stack validate-stack])

(defn validate-pessoa [{:keys [nome apelido nascimento stack] :as pessoa}]
  (when-not (valid? spec pessoa)
    (throw (ex-info "pessoa inválida" {:type :pessoa-invalida}))))
