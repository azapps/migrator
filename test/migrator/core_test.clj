(ns migrator.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [migrator.core :refer :all]
            [migrator.schema :as schema]))

(deftest test-up-down
  (is (= {:up ["foo" "bar"]} ((up ["foo"] ["bar"]) {})))
  (is (= {:down ["foo" "bar"]} ((down ["foo"] ["bar"]) {}))))

(deftest test-defmigration
  (is (= {:name :foo
          :up '("foo")
          :down '("bla")}
         (defmigration foo (up ["foo"]) (down ["bla"]))))
  (is (= {:name :migration
          :up ["foo" "bar"]
          :down ["foo" "bar"]}
         (defmigration migration
           (up ["foo"] ["bar"])
           (down ["foo"] ["bar"])))))

;; some db test

(def postgresql-spec
  {:subprotocol "postgresql"
   :subname     "//localhost:5432/migrator"})
 
(defmigrations example-migrations
  [conn]
  (defmigration user-table
    (up (schema/create conn (schema/table :users
                                          (schema/varchar :name 50)
                                          (schema/varchar :email 50))))
    (down (schema/drop conn (schema/table :users))))
  (defmigration another-table
    (up (schema/create conn (schema/table :tbl
                                          (schema/integer :foo))))
    (down (schema/drop conn (schema/table :tbl)))))

(deftest exec-examples
  (rollback postgresql-spec example-migrations :all) ;; First clean up database
  (jdbc/execute! postgresql-spec ["DROP TABLE migrator_migrations;"])
  (migrate postgresql-spec example-migrations)
  (is (jdbc/query postgresql-spec ["select * from users"])) ;; Should not fail
  (is (jdbc/query postgresql-spec ["select * from tbl"]))
  (rollback postgresql-spec example-migrations)
  (is (thrown? Exception (jdbc/query postgresql-spec ["select * from tbl"])))
  (migrate postgresql-spec example-migrations)
  (is (jdbc/query postgresql-spec ["select * from tbl"]))
  (rollback postgresql-spec example-migrations :all)
  (is (thrown? Exception (jdbc/query postgresql-spec ["select * from tbl"])))
  (is (thrown? Exception (jdbc/query postgresql-spec ["select * from users"])))
  )
