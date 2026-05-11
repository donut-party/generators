["{{member-route-path}}"
 {:name       {{member-route-name}}
  :components {:main [{{member-alias}}/component]}
  ::dfe/on    {:param-change
               [[::dsf/get {::dsf/req {:route-name {{member-route-name}}}
                            ::dfe/xf  [dsf/use-current-route-params]}]]}}]
