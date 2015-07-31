(ns migrator.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [migrator.core :refer :all]
            [migrator.modifiers :as mod]
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

(deftest test-defmigrations
  (is (= (macroexpand-1 '(defmigrations foo [conn]
                           (defmigration foo (up ["foo"]) (down ["bla"]))
                           (defmigration foo2 (up ["foo"] ["bar"]) (down ["bla"]))))
         '(def foo
            (clojure.core/fn [conn]
              (clojure.core/vector (defmigration foo (up ["foo"]) (down ["bla"]))
                                   (defmigration foo2 (up ["foo"] ["bar"]) (down ["bla"]))))))))

;; some db test

(def postgresql-spec
  {:subprotocol "postgresql"
   :subname     "//localhost:5432/migrator"})
 
(defmigrations example-migrations
  [conn]
  (defmigration user-table
    (up (mod/create conn (schema/table :users
                                       (schema/varchar :name 50)
                                       (schema/varchar :email 50))))
    (down (mod/drop conn (schema/table :users))))
  (defmigration another-table
    (up (mod/create conn (schema/table :tbl
                                       (schema/integer :foo))))
    (down (mod/drop conn (schema/table :tbl)))))

(deftest exec-examples
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
