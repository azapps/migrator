; Copyright (c) Nicolas Buduroi. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 which can be found in the file
; epl-v10.html at the root of this distribution. By using this software
; in any fashion, you are agreeing to be bound by the terms of this
; license.
; You must not remove this notice, or any other, from this software.

(ns lobos.backends.postgresql
  "Compiler implementation for PostgreSQL."
  (:refer-clojure :exclude [compile defonce])
  (:require [migrator.schema :as schema]
            [lobos.compiler :refer :all]
            [lobos.utils :refer :all])
  (:import (lobos.ast AlterRenameAction
                      ColumnDefinition
                      DataTypeClause)
           (migrator.schema DataType
                            Schema)))

;; -----------------------------------------------------------------------------

;; ## Compiler

(def ^{:private true} compiler-data-type-aliases
  {:blob :bytea
   :clob :text
   :double :double-precision
   :nclob :text
   :nvarchar :varchar})

(defmethod compile [:postgresql DataTypeClause]
  [expression]
  (let [{:keys [dtype args options]} expression
        {:keys [time-zone]} options
        dtype (first (replace compiler-data-type-aliases [dtype]))
        args (if (#{:bytea :text} dtype) [] args)]
    (unsupported (#{:binary :varbinary} dtype)
      "Use blob instead.")
    (join \space
      (str (as-sql-keyword dtype) (as-list args))
      (when time-zone "WITH TIME ZONE"))))

(defmethod compile [:postgresql ColumnDefinition]
  [definition]
  (let [{:keys [db-spec cname data-type default
                auto-inc not-null others]} definition]
    (apply join \space
           (as-identifier db-spec cname)
           (if auto-inc
             (if (= :bigint (:dtype data-type))
               "BIGSERIAL"
               "SERIAL")
             (compile data-type))
           (when default (str "DEFAULT " (compile default)))
           (not-null-option not-null)
           others)))

(defmethod compile [:postgresql AlterRenameAction]
  [action]
  (let [{:keys [db-spec element]} action
        old-name (:cname element)
        new-name (:others element)]
    (format "RENAME COLUMN %s TO %s"
            (as-identifier db-spec old-name)
            (as-identifier db-spec new-name))))
