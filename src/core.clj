(ns core
  (:require [cheshire.core :as json]
            [clojure.java.shell :as sh]))

(def global-env {"Add" +
                 "Sub" -
                 "Mul" *
                 "Div" /
                 "Rem" rem
                 "Eq" =
                 "Neq" not=
                 "Lt" <
                 "Gt" >
                 "Lte" <=
                 "Gte" >=})

(defn ast
  [file]
  (-> (sh/sh "rinha" file)
      :out
      (json/parse-string keyword)
      :expression))

(declare my-eval)

(defn get-params
  [expression env]
  (list (or (some-> expression :lhs (my-eval env))
            (some-> expression :first (my-eval env)))
        (or (some-> expression :rhs (my-eval env))
            (some-> expression :second (my-eval env)))))

(defn my-eval
  [expression env]
  (case (:kind expression)
    "Print"  (let [value (my-eval (:value expression) env)]
               (println (cond
                          (list? value) (format "(%s, %s)" (first value) (second value))
                          (map? value) "<#closure>"
                          :else value))
               value)
    "Binary" (let [[lhs rhs] (get-params expression env)]
               (case (:op expression)
                 "And" (and lhs rhs)
                 "Or" (or lhs rhs)
                 ((get env (:op expression)) lhs rhs)))
    "Bool" (:value expression)
    "Int" (:value expression)
    "Str" (:value expression)
    "Tuple" (get-params expression env)
    "If" (if (my-eval (:condition expression) env)
           (my-eval (:then expression) env)
           (my-eval (:otherwise expression) env))
    "First" (let [[first] (get-params (:value expression) env)]
              first)
    "Second" (let [[_, second] (get-params (:value expression) env)]
               second)
    "Let" (my-eval (:next expression)
                   (assoc env
                          (-> expression :name :text)
                          (my-eval (:value expression) env)))
    "Var" (get env (:text expression))
    "Function" {:parameters (map :text (:parameters expression))
                :body       (:value expression)}
    "Call" (let [callee    (my-eval (:callee expression) env)
                 arguments (map #(my-eval % env) (:arguments expression))]
             (my-eval
              (:body callee)
              (merge env
                     (into {} (zipmap (:parameters callee) arguments)))))))

(let [expression (ast "resources/combination.rinha")]
  (my-eval expression global-env))
