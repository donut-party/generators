(ns donut.generators
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [donut.generate :as dg]
   [inflections.core :as inflections]
   [rewrite-clj.node :as rn]
   [rewrite-clj.zip :as rz])
  (:import
   java.time.LocalDateTime
   java.time.format.DateTimeFormatter))

(defn entity-name->data
  [{:keys [entity-name route-prefix top]}]
  (let [entity-ns    (-> entity-name inflections/plural dg/->ns)
        es           (inflections/singular entity-name)
        esk          (keyword es)
        ep           (inflections/plural entity-name)
        epk          (keyword ep)
        id-key       (keyword (str es) "id")
        table-name-s (csk/->snake_case_string ep)]
    {:entity-name             esk
     :entity-ns               entity-ns
     :entity-name-singular    es
     :entity-name-plural      ep
     :entity-name-singular-kw esk
     :entity-name-plural-kw   epk
     :endpoint-ns             (symbol (str (dg/->ns top) ".backend.endpoint." ep "-endpoint"))
     :endpoint-ns-alias       (symbol (str ep "-endpoint"))
     :collection-route-path   (str "/" ep)
     :collection-route-name   epk
     :collection-alias        (symbol ep)
     :member-route-path       (str "/" ep "/{" id-key "}")
     :member-route-name       esk
     :member-alias            (symbol es)
     :query-ns                (symbol (str (dg/->ns top) ".backend.query." ep "-query"))
     :query-ns-alias          (symbol (str ep "-query"))
     :query-pk                (keyword table-name-s "id")
     :table-name-s            table-name-s
     :table-name-k            (keyword table-name-s)
     :id-key                  id-key
     :ent-type                esk
     :route-prefix            (or route-prefix "/api/v1")}))

(defn endpoint-route-point
  [routes-file-path data]
  {:id          ::endpoint-route-point
   :description "add route to the endpoint routes vector"
   :destination {:path routes-file-path
                 :dir  "src"}
   :modify      {:path  ['routes (dg/pred vector?)]
                 :edits [dg/append-child-newline
                         dg/append-child-newline
                         rz/append-child]}
   :content     {:template (slurp (io/resource "donut/generators/endpoint/route.clj"))}
   :data        data})

(defmethod dg/generator ::entity-endpoint
  [_ input-data]
  (let [entity-data      (entity-name->data input-data)
        routes-file-path "{{top|file}}/backend/endpoint_routes.clj"]
    {:points
     [{:id          ::create-endpoint-ns
       :description "create the endpoint namespace"
       :destination {:namespace (:endpoint-ns entity-data)
                     :extension "clj"
                     :dir       "src"}
       :content     {:template (slurp (io/resource "donut/generators/endpoint/endpoint_ns.clj"))}
       :data        {:endpoint-ns (:endpoint-ns entity-data)}}

      {:id          ::update-routes-requires
       :description "update the routes namespace :requires to include endpoint namespace"
       :destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['ns :require]
                     :edits [dg/append-child-newline rz/append-child]}
       :content     {:form [(:endpoint-ns entity-data) :as (:endpoint-ns-alias entity-data)]}}

      ;; add a collection route
      (endpoint-route-point
       routes-file-path
       {:route-path     (:collection-route-path entity-data)
        :route-name     (:collection-route-name entity-data)
        :ent-type       (:ent-type entity-data)
        :id-key         (:id-key entity-data)
        :route-handlers (str (:endpoint-ns-alias entity-data) "/collection-handlers")})

      ;; add member route
      (endpoint-route-point
       routes-file-path
       {:route-path     (:member-route-path entity-data)
        :route-name     (:member-route-name entity-data)
        :ent-type       (:ent-type entity-data)
        :id-key         (:id-key entity-data)
        :route-handlers (str (:endpoint-ns-alias entity-data) "/member-handlers")})]

     :data-schema [:map
                   [:entity-name {:optional? false}]]
     :data        entity-data}))

;;---
;; endpoint-tests
;;---

(defn ->potato-prefix
  [s]
  (->> (str/split (name s) #"[^a-zA-Z]")
       (map first)
       (apply str)
       (keyword)))

(defn node-merge
  "merges in all nodes from a map into loc"
  [loc map-node]
  (let [mloc (rz/of-node map-node)]
    (loop [new-loc       (rz/append-child loc (rn/newlines 1))
           navigated-map (rz/next mloc)]
      (if (rz/end? navigated-map)
        new-loc
        (recur (rz/append-child new-loc (rz/node navigated-map))
               (rz/right* navigated-map))))))

(defmethod dg/generator ::entity-endpoint-tests
  [_ {:keys [top entity-name] :as input-data}]
  (let [entity-data       (entity-name->data input-data)
        test-endpoint-ns  (symbol (str (:endpoint-ns entity-data) "-test"))
        fixtures-ns       (symbol (str (dg/->ns top) ".fixtures"))
        potato-prefix     (->potato-prefix entity-name)
        potato-prefix-sym (symbol potato-prefix)]
    {:points
     [{:id          ::create-endpoint-test-ns
       :description "create the endpoint test namespace"
       :destination {:namespace test-endpoint-ns
                     :extension "clj"
                     :dir       "test"}
       :content     {:template (slurp (io/resource "donut/generators/endpoint/endpoint_test_ns.clj"))}
       :data        {:test-endpoint-ns test-endpoint-ns}}

      ;; update fixtures
      {:id          ::update-datapotato-fixture
       :description "add an entity schema to the datapotato fixture"
       :destination {:namespace fixtures-ns
                     :extension "clj"
                     :dir       "test"}
       :content     {:template (slurp (io/resource "donut/generators/endpoint/datapotato_fixture.clj"))}
       :modify      {:path  ['potato-schema (dg/pred map?)]
                     :edits [node-merge]}}]

     :data-schema [:map
                   [:endpoint-name {:optional? false}]]
     :data        (assoc entity-data
                         :fixtures-ns fixtures-ns
                         :potato-prefix potato-prefix
                         :potato-prefix-sym potato-prefix-sym)}))

;;---
;; ui
;;---

(defn entity-component-point
  [ns]
  {:id          ::entity-frontend-component
   :description "creates file for entity frontend component"
   :destination {:namespace ns
                 :extension "cljs"
                 :dir       "src"}
   :content     {:template (slurp (io/resource "donut/generators/frontend/component_ns.cljs"))}
   :data        {:ns ns}})

(defmethod dg/generator ::entity-ui
  [_ {:keys [top] :as input-data}]
  (let [entity-data      (entity-name->data input-data)
        ns-prefix        (str (dg/->ns top) ".frontend.components." (:entity-name-singular entity-data))
        list-ns          (symbol (str ns-prefix ".list"))
        show-ns          (symbol (str ns-prefix ".show"))
        routes-file-path "{{top|file}}/frontend/frontend_routes.cljs"]
    {:points
     [;; add compnent files
      (entity-component-point list-ns)
      (entity-component-point show-ns)

      ;; update frontend routes: add requires
      {:id          ::update-frontend-routes-requires-collection
       :description "update the frontend routes namespace :requires to include collection component namespaces"
       :destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['ns :require]
                     :edits [dg/append-child-newline rz/append-child]}
       :content     {:form [list-ns :as (:collection-alias entity-data)]}}

      {:id          ::update-frontend-routes-requires-member
       :description "update the frontend routes namespace :requires to include member component namespaces"
       :destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['ns :require]
                     :edits [dg/append-child-newline rz/append-child]}
       :content     {:form [show-ns :as (:member-alias entity-data)]}}
      
      ;; update frontend routes: add routes
      {:id          ::add-frontend-route-collection
       :description "add collection route to frontend routes"
       :destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['routes (dg/pred vector?)]
                     :edits [dg/append-child-newline
                             dg/append-child-newline
                             rz/append-child]}
       :content     {:template (slurp (io/resource "donut/generators/frontend/collection_route.cljs"))}
       :data        entity-data}

      {:id          ::add-frontend-route-member
       :description "add member route to frontend routes"
       :destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['routes (dg/pred vector?)]
                     :edits [dg/append-child-newline
                             dg/append-child-newline
                             rz/append-child]}
       :content     {:template (slurp (io/resource "donut/generators/frontend/member_route.cljs"))}
       :data        entity-data}

      ;; update main nav
      {:id          ::update-main-nav
       :description "add link to collection page"
       :destination {:path (str (dg/->file top) "/frontend/app.cljs")
                     :dir  "src"}
       :content     {:template (slurp (io/resource "donut/generators/frontend/main_nav_link.cljs"))}
       :modify      {:path  ['main-nav :ul]
                     :edits [dg/append-child-newline rz/append-child]}
       :data        entity-data}]

     :data-schema [:map
                   [:top {:optional? false}]
                   [:entity-name {:optional? false}]]}))

;;---
;; db
;;---

(defmethod dg/generator ::entity-query
  [_ input-data]
  (let [entity-data (entity-name->data input-data)]
    {:points
     [{:id          ::create-query-ns
       :description "create the query namespace"
       :destination {:namespace (:query-ns entity-data)
                     :extension "clj"
                     :dir       "src"}
       :content {:template (slurp (io/resource "donut/generators/endpoint/query_ns.clj"))}}]
     :data entity-data}))

(defmethod dg/generator ::entity-migrations
  [_ input-data]
  (let [entity-data (entity-name->data input-data)
        timestamp   (.format (LocalDateTime/now)
                             (DateTimeFormatter/ofPattern "yyMMddHHmmss"))
        path-start (str "migrations/" timestamp "-create-" (:table-name-s entity-data))]
    {:points
     [{:id          ::create-entity-migration-up
       :description "create up migration for the entity"
       :destination {:path (str path-start "-up.sql")
                     :dir  "resources"}
       :content     {:template (slurp (io/resource "donut/generators/db/entity-migration-up.sql"))}}
      {:id          ::create-entity-migration-down
       :description "create down migration for the entity"
       :destination {:path (str path-start "-down.sql")
                     :dir  "resources"}
       :content     {:template (slurp (io/resource "donut/generators/db/entity-migration-down.sql"))}}]
     :data entity-data}))

;;---
;; all of them together
;;---

(defmethod dg/generator ::entity-scaffold
  [_ data]
  (let [endpoint      (dg/generator ::entity-endpoint data)
        endpoint-test (dg/generator ::entity-endpoint-tests data)
        query         (dg/generator ::entity-query data)
        migrations    (dg/generator ::entity-migrations data)
        ui            (dg/generator ::entity-ui data)]
    {:points      (vec (concat (:points endpoint)
                               (:points endpoint-test)
                               (:points query)
                               (:points migrations)
                               (:points ui)))
     :data        (merge (:data endpoint)
                         (:data endpoint-test)
                         (:data query)
                         (:data migrations)
                         (:data ui))
     :data-schema [:map
                   [:top {:optional? false}]
                   [:entity-name {:optional? false}]]}))
