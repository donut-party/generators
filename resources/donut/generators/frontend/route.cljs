["{{route-path}}"
 {:name       {{route-name}}
  :components {:main [{{component-ns}}/component]}
  ::dfe/on    {:param-change
               [[::dsf/get {::dsf/req {:route-name {{route-name}}}}]]}}]
