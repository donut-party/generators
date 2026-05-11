(ns donut.generators
  (:require
   [clojure.java.io :as io]
   [donut.generate :as dg]
   [inflections.core :as inflections]
   [rewrite-clj.zip :as rz]))

;; TODO more generic collection-route-name and member-route-name

(defn entity-name->data
  [{:keys [entity-name route-prefix]}]
  (let [entity-ns (-> entity-name inflections/plural dg/->ns)
        es        (inflections/singular entity-name)
        esk       (keyword es)
        ep        (inflections/plural entity-name)
        epk       (keyword ep)
        id-key    (keyword (str es) "id")]
    {:entity-name             entity-name
     :entity-ns               entity-ns
     :entity-name-singular    es
     :entity-name-plural      ep
     :entity-name-singular-kw esk
     :entity-name-plural-kw   epk
     :collection-route-path   (str "/" ep)
     :collection-route-name   epk
     :collection-alias        (symbol ep)
     :member-route-path       (str "/" (str ep "/{" id-key "}"))
     :member-route-name       esk
     :member-alias            (symbol es)
     :id-key                  id-key
     :ent-type                esk
     :route-prefix            (or route-prefix "/api/v1")}))

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
  [_ {:keys [top] :as input-data}]
  (let [entity-data      (entity-name->data input-data)
        endpoint-ns      (symbol (str (dg/->ns top)
                                      ".backend.endpoint."
                                      (:entity-name-plural entity-data)
                                      "-endpoint"))
        routes-file-path "{{top|file}}/backend/endpoint_routes.clj"
        endpoint-alias   (:collection-alias entity-data)]
    {:points
     [ ;; generate the endpoint file
      {:destination {:namespace endpoint-ns
                     :extension "clj"
                     :dir       "src"}
       :content     {:template (slurp (io/resource "donut/generators/endpoint/endpoint_ns.clj"))}
       :data        {:endpoint-ns endpoint-ns}}

      ;; update the routes namespace requires
      {:destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['ns :require]
                     :edits [dg/append-child-newline rz/append-child]}
       :content     {:form [endpoint-ns :as endpoint-alias]}}

      ;; add a collection route
      (endpoint-route-point
       routes-file-path
       {:route-path     (:collection-route-path entity-data)
        :route-name     (:collection-route-name entity-data)
        :ent-type       (:ent-type entity-data)
        :id-key         (:id-key entity-data)
        :route-handlers (str endpoint-alias "/collection-handlers")})

      ;; add member route
      (endpoint-route-point
       routes-file-path
       {:route-path     (:member-route-path entity-data)
        :route-name     (:member-route-name entity-data)
        :ent-type       (:ent-type entity-data)
        :id-key         (:id-key entity-data)
        :route-handlers (str endpoint-alias "/member-handlers")})]

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
      {:destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['ns :require]
                     :edits [dg/append-child-newline rz/append-child]}
       :content     {:form [list-ns :as (:collection-alias entity-data)]}}

      {:destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['ns :require]
                     :edits [dg/append-child-newline rz/append-child]}
       :content     {:form [show-ns :as (:member-alias entity-data)]}}
      
      ;; update frontend routes: add routes
      {:destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['routes (dg/pred vector?)]
                     :edits [dg/append-child-newline
                             dg/append-child-newline
                             rz/append-child]}
       :content     {:template (slurp (io/resource "donut/generators/frontend/collection_route.cljs"))}
       :data        entity-data}

      {:destination {:path routes-file-path
                     :dir  "src"}
       :modify      {:path  ['routes (dg/pred vector?)]
                     :edits [dg/append-child-newline
                             dg/append-child-newline
                             rz/append-child]}
       :content     {:template (slurp (io/resource "donut/generators/frontend/member_route.cljs"))}
       :data        entity-data}

      ;; update main nav
      {:destination {:path (str (dg/->file top) "/frontend/app.cljs")
                     :dir  "src"}
       :content     {:template (slurp (io/resource "donut/generators/frontend/main_nav_link.cljs"))}
       :modify      {:path  ['main-nav :ul]
                     :edits [dg/append-child-newline rz/append-child]}
       :data        entity-data}]

     :data-schema [:map
                   [:top {:optional? false}]
                   [:entity-name {:optional? false}]]}))
