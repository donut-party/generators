(ns donut.generators-test
  (:require
   [clojure.test :refer [deftest is]]
   [donut.generate :as dg]
   [donut.generators :as dgg]))

(deftest entity-endpoint-test
  (is (= [{:contents          
           "(ns generate-test.backend.endpoint.users-endpoint)

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
",
           :file-path "src/generate_test/backend/endpoint/users_endpoint.clj"}
          {:contents
           "(ns x (:require 
 [generate-test.backend.endpoint.users-endpoint :as users-endpoint]))",
           :file-path "src/generate_test/backend/endpoint_routes.clj"}
          {:contents
           "(def routes [
 
 [\"/api/v1/users\"
 (merge {:name     :users
         :ent-type :user
         :id-key   :user/id}
        users-endpoint/collection-handlers)]])",
           :file-path "src/generate_test/backend/endpoint_routes.clj"}
          {:contents
           "(def routes [
 
 [\"/api/v1/users/{:user/id}\"
 (merge {:name     :user
         :ent-type :user
         :id-key   :user/id}
        users-endpoint/member-handlers)]])",
           :file-path "src/generate_test/backend/endpoint_routes.clj"}]
         (dg/generate ::dgg/entity-endpoint
                      {:entity-name 'user
                       :top         "generate-test"}
                      {:read-point  (dg/read-point-test-fn {::dgg/update-routes-requires '(ns x (:require))
                                                            ::dgg/endpoint-route-point   '(def routes [])})
                       :write-point dg/write-point-test}))))

(deftest entity-endpoint-tests-test
  (is (= [{:file-path          
           "test/generate_test/backend/endpoint/users_endpoint_test.clj",
           :contents
           "(ns generate-test.backend.endpoint.users-endpoint-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [donut.datapotato.core :as dc]
   [donut.endpoint.test.harness :as deth]
   [generate-test.fixtures :as fixtures]
   [donut.system :as ds]))

(use-fixtures :each (ds/system-fixture :test))

;; example tests for a hypothetical example-entity:
(deftest get-entities-test
  (dc/with-fixtures fixtures/datapotato-db
    (let [{:keys [u0 u1]} (dc/insert-fixtures {:user [[2]]})]
      (is (= #{u0 u1}
             (set (deth/response :get :users)))))))

(deftest create-entity-test
  (is (= (deth/response :post :users {} {:attr-1 nil})
         [])))

(deftest get-single-entity-test
  (dc/with-fixtures fixtures/datapotato-db
    (let [{:keys [u0]} (dc/insert-fixtures {:user [[2]]})]
      (is (= u0
             (deth/read-body (deth/handle-request :get :user u0)))))))

(deftest update-entity-test
  (dc/with-fixtures fixtures/datapotato-db
    (let [{:keys [u0]} (dc/insert-fixtures {:user [[1]]})]
      (is (= {:attr-1 nil}
             (deth/response :put :user u0 {:attr-1 nil}))))))

(deftest delete-entity-test
  (dc/with-fixtures fixtures/datapotato-db
    (let [{:keys [u0]} (dc/insert-fixtures {:user [[1]]})]
      (deth/handle-request :delete :user u0)
      (is (= []
             (deth/response :get :users))))))
"}
          {:file-path "test/generate_test/fixtures.clj",
           :contents
           "(def potato-schema {
:user  {:prefix   :u
                              :generate {:schema [:map]}
                              :fixtures {:table-name \"user\"}}})"}]
         (dg/generate ::dgg/entity-endpoint-tests
                      {:entity-name 'user
                       :top         "generate-test"}
                      {:read-point  (dg/read-point-test-fn {::dgg/update-datapotato-fixture '(def potato-schema {})})
                       :write-point dg/write-point-test}))))

(deftest entity-ui-test
  (is (= [{:file-path "src/generate_test/frontend/components/user/list.cljs",          
           :contents
           "(ns generate-test.frontend.components.user.list)

(defn component
  []
  [:div
   [:h1 \"component\"]])
"}
          {:file-path "src/generate_test/frontend/components/user/show.cljs",
           :contents
           "(ns generate-test.frontend.components.user.show)

(defn component
  []
  [:div
   [:h1 \"component\"]])
"}
          {:file-path "src/generate_test/frontend/frontend_routes.cljs",
           :contents
           "(ns x (:require 
 [generate-test.frontend.components.user.list :as users]))"}
          {:file-path "src/generate_test/frontend/frontend_routes.cljs",
           :contents
           "(ns x (:require 
 [generate-test.frontend.components.user.show :as user]))"}
          {:file-path "src/generate_test/frontend/frontend_routes.cljs",
           :contents
           "(defroutes routes [
 
 [\"/users\"
 {:name       :users
  :components {:main [users/component]}
  ::dfe/on    {:param-change
               [[::dsf/get {::dsf/req {:route-name :users}}]]}}]])"}
          {:file-path "src/generate_test/frontend/frontend_routes.cljs",
           :contents
           "(defroutes routes [
 
 [\"/users/{:user/id}\"
 {:name       :user
  :components {:main [user/component]}
  ::dfe/on    {:param-change
               [[::dsf/get {::dsf/req {:route-name :user}
                            ::dfe/xf  [dsf/use-current-route-params]}]]}}]])"}
          {:file-path "src/generate_test/frontend/app.cljs",
           :contents
           "(def main-nav [:ul 
 [:li
 [dnc/simple-route-link
  {:data-tip \"users\"
   :route-name :users}
  [:svg {:xmlns \"http://www.w3.org/2000/svg\", :viewBox \"0 0 24 24\", :stroke-linejoin \"round\", :stroke-linecap \"round\", :stroke-width \"2\", :fill \"none\", :stroke \"currentColor\", :class \"my-1.5 inline-block size-4\"}
   [:path {:stroke-linecap \"round\", :stroke-linejoin \"round\", :d \"M16.5 8.25V6a2.25 2.25 0 0 0-2.25-2.25H6A2.25 2.25 0 0 0 3.75 6v8.25A2.25 2.25 0 0 0 6 16.5h2.25m8.25-8.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-7.5A2.25 2.25 0 0 1 8.25 18v-1.5m8.25-8.25h-6a2.25 2.25 0 0 0-2.25 2.25v6\"}]]
  [:span {:class \"is-drawer-close:hidden\"}
   \"users\"]]]])"}]
         (dg/generate ::dgg/entity-ui
                      {:entity-name 'user
                       :top         "generate-test"}
                      {:read-point  (dg/read-point-test-fn {::dgg/update-frontend-routes-requires-collection '(ns x (:require))
                                                            ::dgg/update-frontend-routes-requires-member     '(ns x (:require))
                                                            ::dgg/add-frontend-route-collection              '(defroutes routes [])
                                                            ::dgg/add-frontend-route-member                  '(defroutes routes [])
                                                            ::dgg/update-main-nav                            '(def main-nav [:ul])})
                       :write-point dg/write-point-test}))))
