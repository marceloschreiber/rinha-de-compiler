(ns core
  (:require [cheshire.core :as json]
            [clojure.java.shell :as sh])
  (:refer-clojure :exclude [eval]))

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

(declare eval)

(defn get-params
  [expression env]
  [(or (some-> expression :lhs (eval env))
       (some-> expression :first (eval env)))
   (or (some-> expression :rhs (eval env))
       (some-> expression :second (eval env)))])

(defn eval
  [expression env]
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
                 ((get env (:op expression)) lhs rhs)))
    "Bool" (:value expression)
    "Int" (:value expression)
    "Str" (:value expression)
    "Tuple" (get-params expression env)
    "If" (eval (if (eval (:condition expression) env)
                 (:then expression)
                 (:otherwise expression))
               env)
    "First" (first (get-params (:value expression) env))
    "Second" (second (get-params (:value expression) env))
    "Let" (eval (:next expression)
                (assoc env (-> expression :name :text) (eval (:value expression) env)))
    "Var" (get env (:text expression))
    "Function" {:parameters (map :text (:parameters expression))
                :body       (:value expression)}
    "Call" (let [callee    (eval (:callee expression) env)
                 arguments (map #(eval % env) (:arguments expression))]
             (eval (:body callee)
                   (merge env (zipmap (:parameters callee) arguments))))))

(let [expression (ast "resources/tuple.rinha")]
  (eval expression global-env))
