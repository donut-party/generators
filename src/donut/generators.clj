(ns donut.generators
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.string :as str]
   [donut.generate :as dg]
   [inflections.core :as inflections]
   [rewrite-clj.zip :as rz])
  (:import
   java.time.LocalDateTime
   java.time.format.DateTimeFormatter))

(defn entity-name->data
  [{:keys [entity-name route-prefix top]}]
  (let [entity-ns    (-> entity-name inflections/plural dg/file-path->ns)
        es           (inflections/singular entity-name)
        esk          (keyword es)
        ep           (inflections/plural entity-name)
        epk          (keyword ep)
        table-name-s (csk/->snake_case_string ep)
        ;; next.jdbc qualifies columns with the table name, so the id-key
        ;; uses the snake_case table name: build-element -> :build_elements/id
        id-key       (keyword table-name-s "id")
        id-key-s     (str (symbol table-name-s "id"))]
    {:entity-name             esk
     :entity-ns               entity-ns
     :entity-name-singular    es
     :entity-name-plural      ep
     :entity-name-singular-kw esk
     :entity-name-plural-kw   epk
     :endpoint-ns             (symbol (str (dg/file-path->ns top) ".backend.endpoint." ep "-endpoint"))
     :endpoint-ns-alias       (symbol (str ep "-endpoint"))
     :collection-route-path   (str "/" ep)
     :collection-route-name   epk
     :collection-alias        (symbol ep)
     ;; reitit brace params take a symbol-ish name: {users/id} names the
     ;; param :users/id, while {:users/id} (with colon) creates a param
     ;; literally named ":users/id" that match-by-name can never fill
     :member-route-path       (str "/" ep "/{" id-key-s "}")
     :member-route-name       esk
     :member-alias            (symbol es)
     :query-ns                (symbol (str (dg/file-path->ns top) ".backend.query." ep "-query"))
     :query-ns-alias          (symbol (str ep "-query"))
     :query-pk                (keyword table-name-s "id")
     :table-name-s            table-name-s
     :table-name-k            (keyword table-name-s)
     :id-key                  id-key
     :ent-type                esk
     :route-prefix            (or route-prefix "/api/v1")}))

(defn endpoint-route-point
  [routes-file-path data]
  {:type        [:clojure :modify]
   :id          ::endpoint-route-point
   :description "add route to the endpoint routes vector"
   :destination {:path routes-file-path
                 :dir  "src"}
   :modify      {:path  ['routes (dg/pred vector?)]
                 :edits [dg/append-child-newline
                         dg/append-child-newline
                         [dg/append-string-as-form (dg/clj-template-resource "donut/generators/endpoint/route.clj")]]}
   :data        data})

(defmethod dg/generator ::entity-endpoint
  [_ input-data]
  (let [entity-data      (entity-name->data input-data)
        routes-file-path (dg/interpolated "{{top|file}}/backend/endpoint_routes.clj")]
    {:points
     [{:type        [:clojure :create]
       :id          ::create-endpoint-ns
       :description "create the endpoint namespace"
       :destination {:namespace (:endpoint-ns entity-data)
                     :extension "clj"
                     :dir       "src"}
       :content     {:template (dg/clj-template-resource "donut/generators/endpoint/endpoint_ns.clj")}
       :data        {:endpoint-ns (:endpoint-ns entity-data)}}

      {:type        [:clojure :modify]
       :id          ::update-routes-requires
       :description "update the routes namespace :requires to include endpoint namespace"
       :destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['ns (dg/find-parent :require)]
                     :edits [dg/append-child-newline
                             [rz/append-child [(:endpoint-ns entity-data) :as (:endpoint-ns-alias entity-data)]]]}}

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

(defmethod dg/generator ::entity-endpoint-tests
  [_ {:keys [top entity-name] :as input-data}]
  (let [entity-data       (entity-name->data input-data)
        test-endpoint-ns  (symbol (str (:endpoint-ns entity-data) "-test"))
        fixtures-ns       (symbol (str (dg/file-path->ns top) ".fixtures"))
        potato-prefix     (->potato-prefix entity-name)
        potato-prefix-sym (symbol potato-prefix)]
    {:points
     [{:type        [:clojure :create]
       :id          ::create-endpoint-test-ns
       :description "create the endpoint test namespace"
       :destination {:namespace test-endpoint-ns
                     :extension "clj"
                     :dir       "test"}
       :content     {:template (dg/clj-template-resource "donut/generators/endpoint/endpoint_test_ns.clj")}
       :data        {:test-endpoint-ns test-endpoint-ns}}

      ;; update fixtures
      {:type        [:clojure :modify]
       :id          ::update-datapotato-fixture
       :description "add an entity schema to the datapotato fixture"
       :destination {:namespace fixtures-ns
                     :extension "clj"
                     :dir       "test"}
       :modify      {:path  ['potato-schema (dg/pred map?)]
                     :edits [[dg/node-merge (dg/clj-template-resource "donut/generators/endpoint/datapotato_fixture.clj")]]}}]

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
  {:type        [:clojure :create]
   :id          ::entity-frontend-component
   :description "creates file for entity frontend component"
   :destination {:namespace ns
                 :extension "cljs"
                 :dir       "src"}
   :content     {:template (dg/clj-template-resource "donut/generators/frontend/component_ns.cljs")}
   :data        {:ns ns}})

(defmethod dg/generator ::entity-ui
  [_ {:keys [top] :as input-data}]
  (let [entity-data      (entity-name->data input-data)
        ns-prefix        (str (dg/file-path->ns top) ".frontend.components." (:entity-name-singular entity-data))
        list-ns          (symbol (str ns-prefix ".list"))
        show-ns          (symbol (str ns-prefix ".show"))
        routes-file-path (dg/interpolated "{{top|file}}/frontend/frontend_routes.cljs")]
    {:points
     [;; add component files
      {:type        [:clojure :create]
       :id          ::entity-frontend-component-list
       :description "creates file for entity frontend component list"
       :destination {:namespace list-ns
                     :extension "cljs"
                     :dir       "src"}
       :content     {:template (dg/clj-template-resource "donut/generators/frontend/component_list_ns.cljs")}
       :data        {:ns list-ns}}

      {:type        [:clojure :create]
       :id          ::entity-frontend-component-show
       :description "creates file for entity frontend component show"
       :destination {:namespace show-ns
                     :extension "cljs"
                     :dir       "src"}
       :content     {:template (dg/clj-template-resource "donut/generators/frontend/component_show_ns.cljs")}
       :data        {:ns show-ns}}

      ;; update frontend routes: add requires
      {:type        [:clojure :modify]
       :id          ::update-frontend-routes-requires-collection
       :description "update the frontend routes namespace :requires to include collection component namespaces"
       :destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['ns (dg/find-parent :require)]
                     :edits [dg/append-child-newline
                             [rz/append-child [list-ns :as (:collection-alias entity-data)]]]}}

      {:type        [:clojure :modify]
       :id          ::update-frontend-routes-requires-member
       :description "update the frontend routes namespace :requires to include member component namespaces"
       :destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['ns (dg/find-parent :require)]
                     :edits [dg/append-child-newline
                             [rz/append-child [show-ns :as (:member-alias entity-data)]]]}}

      ;; update frontend routes: add routes
      {:type        [:clojure :modify]
       :id          ::add-frontend-route-collection
       :description "add collection route to frontend routes"
       :destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['routes (dg/pred vector?)]
                     :edits [dg/append-child-newline
                             dg/append-child-newline
                             [dg/append-string-as-form (dg/clj-template-resource "donut/generators/frontend/collection_route.cljs")]]}
       :data        entity-data}

      {:type        [:clojure :modify]
       :id          ::add-frontend-route-member
       :description "add member route to frontend routes"
       :destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['routes (dg/pred vector?)]
                     :edits [dg/append-child-newline
                             dg/append-child-newline
                             [dg/append-string-as-form (dg/clj-template-resource "donut/generators/frontend/member_route.cljs")]]}
       :data        entity-data}

      ;; update main nav
      {:type        [:clojure :modify]
       :id          ::update-main-nav
       :description "add link to collection page"
       :destination {:path (str (dg/ns->file-path top) "/frontend/app.cljs")
                     :dir  "src"}
       :modify      {:path  ['main-nav (dg/find-parent :ul)]
                     :edits [dg/append-child-newline
                             [dg/append-string-as-form (dg/clj-template-resource "donut/generators/frontend/main_nav_link.cljs")]]}
       :data        entity-data}]

     :data-schema [:map
                   [:top {:optional? false}]
                   [:entity-name {:optional? false}]]
     :data        entity-data}))

;;---
;; db
;;---

(defmethod dg/generator ::entity-query
  [_ input-data]
  (let [entity-data (entity-name->data input-data)]
    {:points
     [{:type        [:clojure :create]
       :id          ::create-query-ns
       :description "create the query namespace"
       :destination {:namespace (:query-ns entity-data)
                     :extension "clj"
                     :dir       "src"}
       :content     {:template (dg/clj-template-resource "donut/generators/endpoint/query_ns.clj")}}]
     :data entity-data}))

(defmethod dg/generator ::entity-migrations
  [_ input-data]
  (let [entity-data (entity-name->data input-data)
        timestamp   (.format (LocalDateTime/now)
                             (DateTimeFormatter/ofPattern "yyyyMMddHHmmss"))
        path-start (str "migrations/" timestamp "-create-" (:table-name-s entity-data))]
    {:points
     [{:type        [:clojure :create]
       :id          ::create-entity-migration-up
       :description "create up migration for the entity"
       :destination {:path (str path-start ".up.sql")
                     :dir  "resources"}
       :content     {:template (dg/clj-template-resource "donut/generators/db/entity-migration.up.sql")}}
      {:type        [:clojure :create]
       :id          ::create-entity-migration-down
       :description "create down migration for the entity"
       :destination {:path (str path-start ".down.sql")
                     :dir  "resources"}
       :content     {:template (dg/clj-template-resource "donut/generators/db/entity-migration.down.sql")}}]
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
