(ns {{test-endpoint-ns}}
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [donut.datapotato.core :as dc]
   [donut.endpoint.test.harness :as deth]
   [{{fixtures-ns}} :as fixtures]
   [donut.system :as ds]))

(use-fixtures :each (ds/system-fixture :test))

(deftest get-entities-test
  (dc/with-fixtures fixtures/datapotato-db
    (let [{:keys [{{potato-prefix-sym}}0 {{potato-prefix-sym}}1]} (dc/insert-fixtures {{{entity-name-singular-kw}} [[2]]})]
      (is (= #{{{potato-prefix-sym}}0 {{potato-prefix-sym}}1}
             (set (deth/response :get {{collection-route-name}})))))))

(deftest create-entity-test
  (is (= ["new name"]
         (map :{{table-name-s}}/name
              (deth/response :post {{collection-route-name}} {} {:{{table-name-s}}/name "new name"})))))

(deftest get-single-entity-test
  (dc/with-fixtures fixtures/datapotato-db
    (let [{:keys [{{potato-prefix-sym}}0]} (dc/insert-fixtures {{{entity-name-singular-kw}} [[2]]})]
      (is (= {{potato-prefix-sym}}0
             (deth/read-body (deth/handle-request :get {{member-route-name}} {{potato-prefix-sym}}0)))))))

(deftest update-entity-test
  (dc/with-fixtures fixtures/datapotato-db
    (let [{:keys [{{potato-prefix-sym}}0]} (dc/insert-fixtures {{{entity-name-singular-kw}} [[1]]})]
      (is (= (assoc {{potato-prefix-sym}}0 :{{table-name-s}}/name "updated")
             (deth/response :put {{member-route-name}} {{potato-prefix-sym}}0 {:{{table-name-s}}/name "updated"}))))))

(deftest delete-entity-test
  (dc/with-fixtures fixtures/datapotato-db
    (let [{:keys [{{potato-prefix-sym}}0]} (dc/insert-fixtures {{{entity-name-singular-kw}} [[1]]})]
      (deth/handle-request :delete {{member-route-name}} {{potato-prefix-sym}}0)
      (is (= []
             (deth/response :get {{collection-route-name}}))))))
