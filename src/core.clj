(ns core
  (:require [cheshire.core :as json])
  (:refer-clojure :exclude [eval])
  (:gen-class))

(def global-env {"Sub" -
                 "Mul" *
                 "Rem" rem
                 "Eq" =
                 "Neq" not=
                 "Lt" <
                 "Gt" >
                 "Lte" <=
                 "Gte" >=})

(declare eval)

(defn get-params
  [expression env]
  [(or (some-> expression :lhs (eval env))
       (some-> expression :first (eval env)))
   (or (some-> expression :rhs (eval env))
       (some-> expression :second (eval env)))])

(defn eval
  [expression env]
  (loop [expression expression
         env env]
    (case (:kind expression)
      "Print"  (let [value (eval (:value expression) env)]
                 (println (cond
                            (coll? value) (format "(%s, %s)" (first value) (second value))
                            (map? value) "<#closure>"
                            :else value))
                 value)
      "Binary" (let [[lhs rhs] (get-params expression env)]
                 (case (:op expression)
                   "And" (and lhs rhs)
                   "Or" (or lhs rhs)
                   "Add" (if (and (number? lhs) (number? rhs))
                           (+ lhs rhs)
                           (str lhs rhs))
                   "Div" (Math/floorDiv lhs rhs)
                   ((get env (:op expression)) lhs rhs)))
      "Bool" (:value expression)
      "Int" (:value expression)
      "Str" (:value expression)
      "Tuple" (get-params expression env)
      "If" (recur (if (eval (:condition expression) env)
                    (:then expression)
                    (:otherwise expression))
                  env)
      "First"  (first (eval (:value expression) env))
      "Second" (second (eval (:value expression) env))
      "Let" (recur (:next expression)
                   (assoc env (-> expression :name :text) (eval (:value expression) env)))
      "Var" (get env (:text expression))
      "Function" {:parameters (map :text (:parameters expression))
                  :body       (:value expression)}
      "Call" (let [callee    (eval (:callee expression) env)
                   arguments (map #(eval % env) (:arguments expression))]
               (recur (:body callee)
                      (merge env (zipmap (:parameters callee) arguments)))))))

(defn -main [& _args]
  (-> "/var/rinha/source.rinha.json"
      slurp
      (json/parse-string keyword)
      :expression
      (eval global-env)))

(comment
  (require '[clojure.java.shell :as sh])

  (defn ast
    [file]
    (-> (sh/sh "rinha" file)
        :out
        (json/parse-string keyword)
        :expression))

  (let [expression (ast "resources/div.rinha")]
    (eval expression global-env))
  #())
