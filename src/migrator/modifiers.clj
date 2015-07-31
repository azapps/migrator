(ns migrator.modifiers
  (:refer-clojure :exclude [alter drop])
  (:require [lobos.utils :refer [check-valid-options]]
            [lobos.compiler :as compiler]
            [migrator.schema :as schema]))

(defn- compile-stmt
  [stmt]
  (map #(vector (compiler/compile %)) stmt))

(defn create
  "Builds a create statement with the given schema element. See the
  `lobos.schema` namespace for more details on schema elements
  definition. e.g.:

    user> (create db-spec (table :foo (integer :a)))"
  [db-spec element]
  (compile-stmt
   (schema/build-create-statement element db-spec)))

(defn alter
  "Builds an alter statement with the given schema element. There's
  four types of alter actions: `:add`, `:drop`, `:modify` and
  `:rename`. See the `lobos.schema` namespace for more details on
  schema elements definition. e.g.:

    user> (alter :add (table :foo (integer :a)))
    user> (alter :modify (table :foo (column :a [:default 0])))
    user> (alter :rename (table :foo (column :a :to :b)))"
  [db-spec action element]
  (check-valid-options action :add :drop :modify :rename)
  (compile-stmt
   (schema/build-alter-statement element action db-spec)))

(defn drop
  "Builds a drop statement with the given schema element. It can take
  an optional `behavior` argument, when `:cascade` is specified drops
  all elements relying on the one being dropped. e.g.:

    user> (drop db-spec (table :foo) :cascade)"
  [db-spec element & [behavior]]
  [[(compiler/compile
     (schema/build-drop-statement element behavior db-spec))]])

