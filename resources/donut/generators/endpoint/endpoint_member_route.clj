["{{route-prefix}}/{{endpoint-name}}/{{{endpoint-name-singular}}/id}"
 (merge {:name     {{endpoint-name-singular-kw}}
         :ent-type {{endpoint-name-singular-kw}}
         :id-key   {{endpoint-name-singular-kw}}/id}
        {{endpoint-name}}/member-handlers)]
