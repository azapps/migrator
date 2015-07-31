# Migrator

Migrator is a SQL migration library written in Clojure. It is based on
[Lobos](https://github.com/budu/lobos/) and thus supports currently
H2, MySQL, PostgreSQL, SQLite and SQL Server. Instead of forcing you
to use a specific DSL it takes an arbitrary JDBC-compatible structure
and executes it

The difference to Lobos is that Migrator gives you more flexibility
how to formulate the queries and how to connect to your database. It
does not store global connections and let you manage them yourself.

## Features

* A comprehensive data definition language DSL. Or use your own DSL
* Migrations for schema changes.
* Manage the db connections yourself

## Installation

Lobos is available through [Clojars](https://clojars.org/).

#### `project.clj`
```clojure
:dependencies [[migrator "1.0.0"]]
```


## Usage

Here's a small tutorial on how to use Migrator.


```clj
(defmigrations user-migrations
     (defmigration create-users
       (up (create
            (table :users
                   (integer :id :primary-key)
                   (varchar :name 100)))
           ["CREATE TABLE \"something-else\";"])
       (down
        (drop (table :users))))
     (defmigration create-comment
       (up (create
            (table :comments
                   (integer :id :primary-key)
                   (text :comment))))
       (down
        (drop (table :comments)))))
```

To execute the migrations you can just do:

```clj
(migrate db-conn user-migrations) ;; Migrate
(rollback db-conn user-migrations) ;; Rollback 1 migration
(rollback db-conn user-migrations :all) ;; Rollback all migrations
```

## License

Copyright © 2015 Anatolij Zelenin

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
