(ns donut.generators
  (:require
   [clojure.java.io :as io]
   [donut.generate :as dg]
   [inflections.core :as inflections]
   [rewrite-clj.zip :as rz]))

;; collection
;; member

(defn entity-name->data
  [{:keys [entity-name route-prefix]}]
  (let [entity-ns                      (-> entity-name inflections/plural dg/->ns)
        es                             (inflections/singular entity-name)
        esk                            (keyword es)
        ep                             (inflections/plural entity-name)
        epk                            (keyword ep)
        id-key                         (str es "/id")
        route-prefix                   (or route-prefix "/api/v1")
        endpoint-collection-route-path (str route-prefix "/" ep)
        endpoint-member-route-path     (str route-prefix "/" ep "/{" id-key "}")]
    {:entity-ns                      entity-ns
     :entity-name-singular           es
     :entity-name-plural             ep
     :entity-name-singular-kw        esk
     :entity-name-plural-kw          epk
     :endpoint-collection-route-path endpoint-collection-route-path
     :endpoint-collection-route-name epk
     :endpoint-member-route-path     endpoint-member-route-path
     :endpoint-member-route-nam      esk
     :id-key                         id-key
     :ent-type                       esk}))

(defn endpoint-route-point
  [routes-file-path data]
  {:destination {:path routes-file-path
                 :dir  "src"}
   :modify      {:path  ['routes (dg/pred vector?)]
                 :edits [dg/append-child-newline
                         dg/append-child-newline
                         rz/append-child]}
   :content     {:template (slurp (io/resource "donut/generators/endpoint/route.clj"))}
   :data        data})

(defmethod dg/generator ::entity-endpoint
  [_ {:keys [top entity-name] :as input-data}]
  (let [endpoint-ns      (symbol (str (dg/->ns top) ".backend.endpoint." entity-name "-endpoint"))
        routes-file-path "{{top|file}}/backend/endpoint_routes.clj"
        entity-data      (entity-name->data input-data)]
    {:points
     [ ;; generate the endpoint file
      {:destination {:namespace endpoint-ns
                     :extension "clj"
                     :dir       "src"}
       :content     {:template (slurp (io/resource "donut/generators/endpoint/endpoint_ns.clj"))}}

      ;; update the routes namespace requires
      {:destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['ns :require]
                     :edits [dg/append-child-newline rz/append-child]}
       :content     {:form [endpoint-ns :as entity-name]}}

      ;; add a collection route
      (endpoint-route-point
       routes-file-path
       {:route-path     (:endpoint-collection-route-path entity-data)
        :ent-type       (:ent-type entity-data)
        :id-key         (:id-key entity-data)
        :route-handlers (str (:entity-ns entity-data) "/collection-handlers")})

      ;; add member route
      (endpoint-route-point
       routes-file-path
       {:route-path     (:endpoint-member-route-path entity-data)
        :ent-type       (:ent-type entity-data)
        :id-key         (:id-key entity-data)
        :route-handlers (str (:entity-ns entity-data) "/member-handlers")})]

     :data-schema [:map
                   [:endpoint-name {:optional? false}]]
     :data        entity-data}))

(defn entity-component-point
  [ns]
  {:destination {:namespace ns
                 :extension "cljs"
                 :dir       "src"}
   :content     {:template (slurp (io/resource "donut/generators/frontend/component_ns.cljs"))}
   :data        {:ns ns}})

(defn entity-route-point
  [routes-path data]
  {:destination {:path routes-path
                 :dir  "src"}
   :modify      {:path  ['routes (dg/pred vector?)]
                 :edits [dg/append-child-newline
                         dg/append-child-newline
                         rz/append-child]}
   :content     {:template (slurp (io/resource "donut/generators/frontend/route.cljs"))}
   :data        data})

(defmethod dg/generator ::entity-ui
  [_ {:keys [top entity-name] :as input-data}]
  (let [entity-data (entity-name->data input-data)
        _           (prn "entity name" entity-name (:entity-name-singular entity-data) entity-data)
        ns-prefix   (str (dg/->ns top) ".frontend.components." (:entity-name-singular entity-data))
        list-ns     (str ns-prefix ".list")
        show-ns     (str ns-prefix ".show")
        routes-path "{{top|file}}/frontend/frontend_routes.cljs"]
    {:points
     [(entity-component-point list-ns)
      (entity-component-point show-ns)
      (entity-route-point routes-path {:route-name   (:endpoint-collection-route-name entity-data)
                                       :route-path   (str "/" (:entity-name-plural entity-data))
                                       :component-ns list-ns})
      (entity-route-point routes-path {:route-name   (:endpoint-member-route-name entity-data)
                                       :route-path   (str "/" (:entity-name-plural entity-data) "/{" (:id-key entity-data) "}")
                                       :component-ns show-ns})]
     :data-schema [:map
                   [:top {:optional? false}]
                   [:entity-name {:optional? false}]]}))
