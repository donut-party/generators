(ns donut.generators-test
  (:require
   [clojure.test :refer [deftest is]]
   [donut.generate :as dg]
   [donut.generators :as dgg]))

(def generator-input
  {:entity-name 'user
   :top         "generate-test"})

(deftest entity-endpoint-test
  (is (= [{:contents
           "(ns generate-test.backend.endpoint.users-endpoint
  (:require
   [generate-test.backend.query.users-query :as users-query]))

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
  [{:keys [dependencies]}]
  {:status 200
   :body (users-query/find-all (:datasource dependencies))})

(defn collection-post
  [{:keys [all-params dependencies]}]
  {:status 200
   :body   (users-query/insert! (:datasource dependencies) all-params)})

(defn member-get
  [{:keys [all-params dependencies]}]
  {:status 200
   :body (users-query/find-by-id (:datasource dependencies) all-params)})

(defn member-put
  [{:keys [all-params dependencies]}]
  (users-query/update-by-id! (:datasource dependencies) all-params)
  {:status 200
   :body   (users-query/find-by-id (:datasource dependencies) all-params)})

(defn member-delete
  [{:keys [all-params dependencies]}]
  (users-query/delete-by-id! (:datasource dependencies) all-params)
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
",
           :file-path "src/generate_test/backend/endpoint/users_endpoint.clj"}
          {:contents
           "(ns x (:require\n       [generate-test.backend.endpoint.users-endpoint :as users-endpoint]))",
           :file-path "src/generate_test/backend/endpoint_routes.clj"}
          {:contents
           "(def routes [\n             \n             [\"/users\"\n (merge {:name     :users\n         :ent-type :user\n         :id-key   :users/id}\n        users-endpoint/collection-handlers)]])",
           :file-path "src/generate_test/backend/endpoint_routes.clj"}
          {:contents
           "(def routes [\n             \n             [\"/users/{users/id}\"\n (merge {:name     :user\n         :ent-type :user\n         :id-key   :users/id}\n        users-endpoint/member-handlers)]])",
           :file-path "src/generate_test/backend/endpoint_routes.clj"}]
         (dg/generate ::dgg/entity-endpoint
                      generator-input
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

(deftest get-entities-test
  (dc/with-fixtures fixtures/datapotato-db
    (let [{:keys [u0 u1]} (dc/insert-fixtures {:user [[2]]})]
      (is (= #{u0 u1}
             (set (deth/response :get :users)))))))

(deftest create-entity-test
  (is (= [\"new name\"]
         (map :users/name
              (deth/response :post :users {} {:users/name \"new name\"})))))

(deftest get-single-entity-test
  (dc/with-fixtures fixtures/datapotato-db
    (let [{:keys [u0]} (dc/insert-fixtures {:user [[2]]})]
      (is (= u0
             (deth/read-body (deth/handle-request :get :user u0)))))))

(deftest update-entity-test
  (dc/with-fixtures fixtures/datapotato-db
    (let [{:keys [u0]} (dc/insert-fixtures {:user [[1]]})]
      (is (= (assoc u0 :users/name \"updated\")
             (deth/response :put :user u0 {:users/name \"updated\"}))))))

(deftest delete-entity-test
  (dc/with-fixtures fixtures/datapotato-db
    (let [{:keys [u0]} (dc/insert-fixtures {:user [[1]]})]
      (deth/handle-request :delete :user u0)
      (is (= []
             (deth/response :get :users))))))
"}
          {:file-path "test/generate_test/fixtures.clj",
           :contents
           "(def potato-schema {:user {:prefix   :u\n                              :generate {:schema [:map [:users/name string?]]}\n                              :fixtures {:table-name \"users\"}}})"}]
         (dg/generate ::dgg/entity-endpoint-tests
                      generator-input
                      {:read-point  (dg/read-point-test-fn {::dgg/update-datapotato-fixture '(def potato-schema {})})
                       :write-point dg/write-point-test}))))

(deftest entity-ui-test
  (is (= [{:file-path "src/generate_test/frontend/components/user/list.cljs",
           :contents
           "(ns generate-test.frontend.components.user.list
  (:require
   [donut.compose :as dc]
   [donut.frontend.core.utils :as dcu]
   [donut.frontend.events :as dfe]
   [donut.frontend.form.components :as dfc]
   [donut.frontend.form.flow :as dff]
   [donut.frontend.sync.flow :as dsf]
   [re-frame.core :as rf]))

;; TODO move this to the subs namespace
(rf/reg-sub {{entity-name-plural-kw}}
  (fn [db]
    (dcu/entities db {{entity-name-singular-kw}})))

(defn {{entity-name-plural}}
  []
  [:div
   (->> @(rf/subscribe [{{entity-name-plural-kw}}])
        (mapv (fn [x] ^{:key ({{id-key}} x)} [:li ({{entity-name-plural-kw}}/name x)]))
        (into [:ul]))])

(defn component
  []
  [:div {:class \"prose\"}
   [:h1 \"{{entity-name-plural}}\"]
   (dfc/with-form {::dff/form-key :new-{{entity-name-singular}}}
     [:form {:on-submit
             (dcu/prevent-default
                     (fn [_]
                       (*sync-form {::dsf/req {:method     :post
                                               :route-name {{collection-route-name}}}
                                    ::dfe/on  {:success (dc/into [[::dff/clear-form]])}})))}
      [:div
       [*input {:type                  :text
                :class                 [\"input\"]
                :placeholder           \"name\"
                :donut.input/attr-path {{entity-name-plural-kw}}/name}]]])
   [{{entity-name-plural}}]])
"}
          {:file-path "src/generate_test/frontend/components/user/show.cljs",
           :contents
           "(ns generate-test.frontend.components.user.show)

(defn component
  []
  [:div
   [:h1 \"{{entity-name-singular}}\"]])
"}
          {:file-path "src/generate_test/frontend/frontend_routes.cljs",
           :contents
           "(ns x (:require\n       [generate-test.frontend.components.user.list :as users]))"}
          {:file-path "src/generate_test/frontend/frontend_routes.cljs",
           :contents
           "(ns x (:require\n       [generate-test.frontend.components.user.show :as user]))"}
          {:file-path "src/generate_test/frontend/frontend_routes.cljs",
           :contents
           "(defroutes routes [\n                   \n                   [\"/users\"\n {:name       :users\n  :components {:main [users/component]}\n  ::dfe/on    {:param-change\n               [[::dsf/get {::dsf/req {:route-name :users}}]]}}]])"}
          {:file-path "src/generate_test/frontend/frontend_routes.cljs",
           :contents
           "(defroutes routes [\n                   \n                   [\"/users/{users/id}\"\n {:name       :user\n  :components {:main [user/component]}\n  ::dfe/on    {:param-change\n               [[::dsf/get {::dsf/req {:route-name :user}\n                            ::dfe/xf  [dsf/use-current-route-params]}]]}}]])"}
          {:file-path "src/generate_test/frontend/app.cljs",
           :contents
           "(def main-nav [:ul\n               [:li\n [dnc/simple-route-link\n  {:data-tip \"users\"\n   :route-name :users}\n  [:svg {:xmlns \"http://www.w3.org/2000/svg\", :viewBox \"0 0 24 24\", :stroke-linejoin \"round\", :stroke-linecap \"round\", :stroke-width \"2\", :fill \"none\", :stroke \"currentColor\", :class \"my-1.5 inline-block size-4\"}\n   [:path {:stroke-linecap \"round\", :stroke-linejoin \"round\", :d \"M16.5 8.25V6a2.25 2.25 0 0 0-2.25-2.25H6A2.25 2.25 0 0 0 3.75 6v8.25A2.25 2.25 0 0 0 6 16.5h2.25m8.25-8.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-7.5A2.25 2.25 0 0 1 8.25 18v-1.5m8.25-8.25h-6a2.25 2.25 0 0 0-2.25 2.25v6\"}]]\n  [:span {:class \"is-drawer-close:hidden\"}\n   \"users\"]]]])"}]
         (dg/generate ::dgg/entity-ui
                      generator-input
                      {:read-point  (dg/read-point-test-fn {::dgg/update-frontend-routes-requires-collection '(ns x (:require))
                                                            ::dgg/update-frontend-routes-requires-member     '(ns x (:require))
                                                            ::dgg/add-frontend-route-collection              '(defroutes routes [])
                                                            ::dgg/add-frontend-route-member                  '(defroutes routes [])
                                                            ::dgg/update-main-nav                            '(def main-nav [:ul])})
                       :write-point dg/write-point-test}))))

(deftest query-test
  (is (= [{:file-path "src/generate_test/backend/query/users_query.clj",
           :contents
           "(ns generate-test.backend.query.users-query
  (:require
   [honey.sql :as sql]
   [next.jdbc.sql :as jsql]))

(defn find-all
  [db]
  (->> {:select [:*]
        :from   [:users]}
       sql/format
       (jsql/query db)
       (into [])))

(defn find-by-id
  [db {:keys [:users/id]}]
  (->> {:select [:*]
        :from   [:users]
        :where  [:= :users/id id]}
       sql/format
       (jsql/query db)
       first))

(defn insert!
  [db record]
  (let [result (jsql/insert! db :users record)]
    (jsql/query db [\"SELECT * FROM users WHERE rowid = ?\"
                    (first (vals result))])))

(defn delete-by-id!
  [db {:keys [:users/id]}]
  (jsql/delete! db :users [\"id = ?\" id]))

(defn update-by-id!
  [db record]
  (jsql/update! db
                :users
                (dissoc record :users/id)
                (select-keys record  [:users/id])))
"}]
         (dg/generate ::dgg/entity-query
                      {:entity-name 'user
                       :top         "generate-test"}
                      {:write-point dg/write-point-test}))))

(deftest migrations-test
  (let [generated (dg/generate ::dgg/entity-migrations
                               {:entity-name 'user
                                :top         "generate-test"}
                               {:write-point dg/write-point-test})]
    (is (= [{:contents
             "CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL
);
"}
            {:contents "DROP TABLE IF EXISTS users;\n"}]
           (mapv #(select-keys % [:contents]) generated)))))

(deftest scaffold-test
  (is (dg/generate ::dgg/entity-scaffold
                   {:entity-name 'user
                    :top         "generate-test"}
                   {:read-point  (dg/read-point-test-fn {::dgg/update-routes-requires                     '(ns x (:require))
                                                         ::dgg/endpoint-route-point                       '(def routes [])
                                                         ::dgg/update-frontend-routes-requires-collection '(ns x (:require))
                                                         ::dgg/update-frontend-routes-requires-member     '(ns x (:require))
                                                         ::dgg/update-datapotato-fixture                  '(def potato-schema {})
                                                         ::dgg/add-frontend-route-collection              '(defroutes routes [])
                                                         ::dgg/add-frontend-route-member                  '(defroutes routes [])
                                                         ::dgg/update-main-nav                            '(def main-nav [:ul])})
                    :write-point dg/write-point-test})))
