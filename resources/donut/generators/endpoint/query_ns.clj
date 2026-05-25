(ns {{query-ns}}
  (:require
   [honey.sql :as sql]
   [next.jdbc.sql :as jsql]))

(defn find-all
  [db]
  (->> {:select [:*]
        :from   [{{table-name-k}}]}
       sql/format
       (jsql/query db)
       (into [])))

(defn find-by-id
  [db {:keys [{{query-pk}}]}]
  (->> {:select [:*]
        :from   [{{table-name-k}}]
        :where  [:= {{query-pk}} id]}
       sql/format
       (jsql/query db)
       first))

(defn insert!
  [db record]
  (let [result (jsql/insert! db {{table-name-k}} record)]
    (jsql/query db ["SELECT * FROM {{table-name-s}} WHERE rowid = ?"
                    (first (vals result))])))
