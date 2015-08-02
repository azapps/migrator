(ns migrator.core
  (:require [clojure.java.jdbc :as jdbc]
            [lobos.analyzer :as analyzer]
            [migrator.schema :as schema]))


(def migration-table
  "migrator_migrations")

(defn up
  [& statements]
  (fn [m] (assoc m :up (apply concat statements))))
(defn down
  [& statements]
  (fn [m] (assoc m :down (apply concat statements))))

(defmacro defmigration
  [name & body]
  (reduce (fn [acc arg] `(~arg ~acc))
          {:name (keyword name)}
          `~body))

(defmacro defmigrations
  [name args & migrations]
  `(def ~name
     (fn
       ~args
       (vector ~@migrations))))

(defn trace
  [t]
  (prn t)
  t)

(defn exec-stmt
  [conn stmt]
  (doall (map (partial jdbc/execute! conn) stmt)))


(defn migration-executed?
  "Checks wether a specific migration was already executed. If no migration-db exists it will be created"
  [conn mname]
  (if (-> (analyzer/analyze-schema conn)
          :tables
          (get (keyword migration-table)))    
    (let [cnt (jdbc/query conn [(str "SELECT COUNT(*) AS count FROM " (name migration-table) " WHERE migration=?") (name mname)])]
      (not (= 0 (:count (first cnt)))))
    (do
      (exec-stmt conn
                 (schema/create conn (schema/table migration-table
                                                   (schema/varchar :migration 255))))
      false)))

(defn migrate-1
  [conn migration]
  ;; 0. create transaction
  (jdbc/with-db-transaction [tx conn]
    ;; 1. Check if migration was already executed
    (if-not (migration-executed? tx (:name migration))
      (do
        ;; 2. Exec migration
        (exec-stmt tx (:up migration))
        ;; 3. Store migration in db
        (jdbc/insert! tx migration-table {:migration (name (:name migration))})
        {(:name migration) true})
      {(:name migration) false})))

(defn rollback-1
  [conn migration]
  ;; 0. create transaction
  (jdbc/with-db-transaction [tx conn]
    ;; 1. Check if migration was executed  
    (if (trace (migration-executed? tx (:name migration)))
      (do
        ;; 2. Exec rollback
        (exec-stmt tx (:down migration))
        ;; 3. Delete migration from db
        (jdbc/delete! tx migration-table ["migration = ?" (name (:name migration))])
        {(:name migration) true})
      {(:name migration) false})))
(defn migrate
  [conn migrations]
  (doall (map (partial migrate-1 conn) (migrations conn))))
(defn rollback
  [conn migrations & [do-all?]]
  (let [rollbacks (reverse (migrations conn))]
    (if do-all?
      (doall (map (partial rollback-1 conn) rollbacks))
      (some (partial rollback-1 conn) rollbacks))))
