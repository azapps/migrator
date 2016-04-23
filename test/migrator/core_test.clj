(ns migrator.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [migrator.core :refer :all]
            [migrator.schema :as schema]))

(def postgresql-spec
  {:dbtype "postgresql"
   :dbname "migrator"})

;; test macro structure

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

;; "real" migrations
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

(defn db-fixtures
  [f]
  ;; Rollback it in the beginning s.t. we can inspect the database
  ;; after running the tests
  (rollback postgresql-spec example-migrations :all)
  (f))
(use-fixtures :each db-fixtures)

;; Test example walk through
(deftest exec-examples
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

;; Test up-to-date?
(deftest test-up-to-date
  (migrate postgresql-spec example-migrations)
  (is (up-to-date? postgresql-spec example-migrations false))
  (is (up-to-date? postgresql-spec example-migrations true)))

(deftest test-up-to-date-inconsistent-migrations
  (migrate postgresql-spec example-migrations)
  ;; Rollback one step
  (rollback postgresql-spec example-migrations)
  (is (not (up-to-date? postgresql-spec example-migrations false)))
  (is (not (up-to-date? postgresql-spec example-migrations true)))
  ;; Inserting now a new migration should fail for both again
  (jdbc/insert! postgresql-spec migration-table
                {:migration "no-migration"})
  (is (not (up-to-date? postgresql-spec example-migrations false)))
  (is (not (up-to-date? postgresql-spec example-migrations true)))
  (jdbc/delete! postgresql-spec migration-table
                ["migration = ?" "no-migration"]))

(deftest test-up-to-date-with-newer-migration
  ;; migrate and insert something new depending on accept-newer the
  ;; result should differ
  (migrate postgresql-spec example-migrations)
  (jdbc/insert! postgresql-spec migration-table
                {:migration "no-migration"})
  (is (not (up-to-date? postgresql-spec example-migrations false)))
  (is (up-to-date? postgresql-spec example-migrations true))
  (jdbc/delete! postgresql-spec migration-table
                ["migration = ?" "no-migration"]))
