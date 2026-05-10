(ns donut.generators
  (:require
   [clojure.java.io :as io]
   [donut.generate :as dg]
   [inflections.core :as inflections]
   [rewrite-clj.zip :as rz]))

(defmethod dg/generator-points ::endpoint
  [_ {:keys [top endpoint-name]}]
  (let [endpoint-ns (symbol (str (dg/->ns top)
                                 ".backend.endpoint."
                                 endpoint-name
                                 "-endpoint"))]
    {:points
     [;; generate the endpoint file
      {:destination {:namespace "{{endpoint-ns}}"
                     :extension "clj"
                     :dir       "src"}
       :content     {:template (slurp (io/resource "donut/generators/endpoint/endpoint_ns.clj"))}}

      ;; update the routes namespaces
      {:destination {:path    "{{top|file}}/backend/endpoint_routes.clj"
                     :dir     "src"
                     :rewrite {:path    ['ns :require :clj (dg/pred vector?)]
                               :actions [dg/append-newline-child rz/append-child]}}
       :content     {:form [endpoint-ns :as endpoint-name]}}

      ;; add a collection route
      {:destination {:path    "{{top|file}}/cross/endpoint_routes.cljc"
                     :dir     "src"
                     :rewrite {:path    ['routes (dg/pred vector?)]
                               :actions [dg/append-newline-child rz/append-child]}}
       :content     {:template (slurp (io/resource "donut/generators/endpoint/endpoint_collection_route.clj"))}}

      ;; add member route
      {:destination {:path    "{{top/file}}/cross/endpoint_routes.cljc"
                     :dir     "src"
                     :rewrite {:path    ['routes vector?]
                               :actions [:append-newline :append-child]}}
       :content     {:template (slurp (io/resource "donut/generators/endpoint/endpoint_member_route.clj"))}}]

     :data-schema [:map
                   [:endpoint-name {:optional? false}]]
     :data        {:endpoint-ns               endpoint-ns
                   :endpoint-name-singular    (-> endpoint-name inflections/singular)
                   :endpoint-name-plural      (-> endpoint-name inflections/plural)
                   :endpoint-name-singular-kw (-> endpoint-name inflections/singular keyword)
                   :endpoint-name-plural-kw   (-> endpoint-name inflections/plural keyword)
                   :route-prefix              "/api/v1"}}))
