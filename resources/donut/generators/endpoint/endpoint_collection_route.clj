[\"{{route-prefix}}/{{endpoint-name}}\"
 (merge {:name     {{endpoint-name-plural-kw}}
         :ent-type {{endpoint-name-singular-kw}}
         :id-key   {{endpoint-name-singular-kw}}/id}
        {{endpoint-name}}/collection-handlers)]
