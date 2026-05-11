["{{collection-route-path}}"
 {:name       {{collection-route-name}}
  :components {:main [{{collection-alias}}/component]}
  ::dfe/on    {:param-change
               [[::dsf/get {::dsf/req {:route-name {{collection-route-name}}}}]]}}]
