(ns {{endpoint-ns}}
  (:require
   [{{query-ns}} :as {{query-ns-alias}}]))

;;---
;; schemas
;;---
;; malli schemas for request and response validation and coercion

(def collection-parameters
  {:path [:map
          ;; example: [:todo_list/id int?]
          ]})

(def member-parameters
  {:path [:map
          [{{id-key}} int?]]})

;;---
;; endpoint handlers
;;---
;; request handlers for specific HTTP methods for a route

(defn collection-get
  [{:keys [dependencies]}]
  {:status 200
   :body ({{query-ns-alias}}/find-all (:datasource dependencies))})

(defn collection-post
  [{:keys [all-params dependencies]}]
  {:status 200
   :body   ({{query-ns-alias}}/insert! (:datasource dependencies) all-params)})

(defn member-get
  [{:keys [all-params dependencies]}]
  {:status 200
   :body ({{query-ns-alias}}/find-by-id (:datasource dependencies) all-params)})

(defn member-put
  [{:keys [all-params dependencies]}]
  ({{query-ns-alias}}/update-by-id! (:datasource dependencies) all-params)
  {:status 200
   :body   ({{query-ns-alias}}/find-by-id (:datasource dependencies) all-params)})

(defn member-delete
  [{:keys [all-params dependencies]}]
  ({{query-ns-alias}}/delete-by-id! (:datasource dependencies) all-params)
  {:status 200
   :body   [:delete-routed-entity]})

;;---
;; route handlers
;;---
;; reitit/ring handlers tying together request method with endpoint handlers

(def collection-handlers
  {:get
   {:parameters collection-parameters
    :handler    #'collection-get}

   :post
   {:parameters collection-parameters
    :handler    #'collection-post}})

(def member-handlers
  {:get
   {:parameters member-parameters
    :handler    #'member-get}

   :put
   {:parameters member-parameters
    :handler    #'member-put} 

   :delete
   {:parameters member-parameters
    :handler    #'member-delete}})
