# Migrator

[![Clojars Project](https://img.shields.io/clojars/v/migrator.svg)](https://clojars.org/migrator)

Migrator is a SQL migration library written in Clojure. It is based on
[Lobos](https://github.com/budu/lobos/) and thus supports currently
H2, MySQL, PostgreSQL, SQLite and SQL Server (Currently only
PostgreSQL and MySQL are tested). Instead of forcing you to use a
specific DSL it takes an arbitrary JDBC-compatible structure and
executes it.

The difference to Lobos is that Migrator gives you more flexibility
how to formulate the queries and how to connect to your database. It
does not store global connections and let you manage them yourself.

## Features

* A comprehensive data definition language DSL. Or use your own DSL
* Migrations for schema changes.
* Manage the db connections yourself

## Usage

Here's a small example on how to use Migrator.

```clj
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
```

To execute the migrations you can just do:

```clj
(migrate db-conn user-migrations) ;; Migrate
(rollback db-conn user-migrations) ;; Rollback 1 migration
(rollback db-conn user-migrations :all) ;; Rollback all migrations
```

## License

Copyright © 2016 Anatoly Zelenin, Georg Semmler

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
