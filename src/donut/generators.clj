(ns donut.generators
  (:require
   [donut.generate :as dg]
   [inflections.core :as inflections]))

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
       :content     {:template "(ns {{endpoint-ns}})

(def collection-parameters
  {:path [:map
          ;; example: [:todo_list/id int?]
          ]})

(def collection-parameters
  {:path [:map
          ;; example: [:todo_list/id int?]
          ]})

(def collection-handlers
  {:get
   {:parameters collection-parameters
    :handler
    (fn [{:keys [db all-params]}]
      {:status 200
       :body   [{:entity/id 0}]})}

   :post
   {:parameters collection-parameters
    :handler
    (fn [{:keys [all-params db]}]
      ;; insert a record
      {:status 200
       :body   []})}})

(def member-parameters
  {:path [:map]})

(def member-handlers
  {:get
   {:parameters member-parameters
    :handler    (fn [{:keys [all-params db]}]
                  {:status 200
                   :body   {:entity/id 0}}
                  )}

   :put
   {:parameters member-parameters
    :handler    (fn []
                  ;; TODO update entity
                  {:status 200
                   :body   {:entity/id 0}})

    :delete
    {:parameters member-parameters
     :handler    (fn [{:keys [all-params db]}]
                   ;; TODO delete entity
                   )}
    }})"}}

      ;; update the routes namespaces
      {:destination {:path    "{{top/file}}/cross/endpoint_routes.cljc"
                     :dir     "src"
                     :rewrite {:path    ['ns :require :clj vector?]
                               :actions [:append-newline :append-child]}}
       :content     {:form [endpoint-ns :as endpoint-name]}}

      ;; add a collection route
      {:destination {:path    "{{top/file}}/cross/endpoint_routes.cljc"
                     :dir     "src"
                     :rewrite {:path    ['routes vector?]
                               :actions [:append-newline :append-child]}}
       :content     {:template "
   [\"{{route-prefix}}/{{endpoint-name}}\"
    {:name     {{endpoint-name-plural-kw}}
     :ent-type {{endpoint-name-singular-kw}}
     :id-key   {{endpoint-name-singular-kw}}/id}
    #?(:clj {{endpoint-name}}/collection-handlers)]"}}

      ;; add member route
      {:destination {:path    "{{top/file}}/cross/endpoint_routes.cljc"
                     :dir     "src"
                     :rewrite {:path    ['routes vector?]
                               :actions [:append-newline :append-child]}}
       :content     {:template "
   [\"{{route-prefix}}/{{endpoint-name}}/{{{endpoint-name-singular}}/id}\"
    {:name     {{endpoint-name-singular-kw}}
     :ent-type {{endpoint-name-singular-kw}}
     :id-key   {{endpoint-name-singular-kw}}/id}
    #?(:clj {{endpoint-name}}/member-handlers)]"}}]

     :data-schema [:map
                   [:endpoint-name {:optional? false}]]
     :data        {:endpoint-ns               endpoint-ns
                   :endpoint-name-singular    (-> endpoint-name inflections/singular)
                   :endpoint-name-plural      (-> endpoint-name inflections/plural)
                   :endpoint-name-singular-kw (-> endpoint-name inflections/singular keyword)
                   :endpoint-name-plural-kw   (-> endpoint-name inflections/plural keyword)
                   :route-prefix              "/api/v1"}}))
