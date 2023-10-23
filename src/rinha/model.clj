(ns rinha.model
  (:require [clova.core :refer :all]))

(validate [:nome required? stringy? [longer? 0] [shorter? 101]
          :apelido required? stringy? [longer? 0] [shorter? 33]
          :nascimento required? [matches? #"\d{4}-\d{2}-\d{2}"]]
          {:apelido "joao"
           :nome "231"
           :nascimento "1985-09-01"
           :stack nil})


(results [:nome stringy? [longer? 0] [shorter? 2]
          :apelido stringy?]
         {:nome "a" :apelido 1})

