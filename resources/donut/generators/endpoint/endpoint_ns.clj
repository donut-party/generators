(ns {{endpoint-ns}})

;;---
;; schemas
;;---
;; malli schemas for request and response validation and coercion

(def collection-parameters
  {:path [:map
          ;; example: [:todo_list/id int?]
          ]})

(def member-parameters
  {:path [:map]})

;;---
;; endpoint handlers
;;---
;; request handlers for specific HTTP methods for a route

(defn collection-get
  [{:keys [all-params dependencies]}]
  ;; TODO query for entities
  {:status 200
   :body []
   ;; example of how you might return collection entities
   #_(query/find-all (:datasource dependencies) all-params)}
  )

(defn collection-post
  [{:keys [all-params db]}]
  ;; TODO insert a record
  {:status 200
   :body   []})

(defn member-get
  [{:keys [all-params dependencies]}]
  ;; TODO query for entity
  {:status 200
   :body {}
   ;; example of how you might return collection entities
   #_(query/find-one (:datasource dependencies) all-params)}
  )

(defn member-put
  [{:keys [all-params dependencies]}]
  ;; TODO update entity
  {:status 200
   :body   {}})

(defn member-delete
  [{:keys [all-params dependencies]}]
  ;; TODO delete entity
  {:status 200
   :body   [:delete-routed-entity]})

;;---
;; route handlers
;;---
;; reitit/ring handlers tying together method with endpoint handlers

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
