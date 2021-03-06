(ns migrator.core
  (:require [clojure.java.jdbc :as jdbc]
            [analytor.core :as analyzer]
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

(defn exec-stmt
  [conn stmt]
  (doall (map (partial jdbc/execute! conn) stmt)))

(defn fix-connection
  [conn]
  (let [conn (update conn :dbtype #(or % (:subprotocol conn)))]
    (schema/autorequire-backend conn)
    conn))


(defn migration-executed?
  "Checks wether a specific migration was already executed. If no migration-db exists it will be created"
  [conn mname]
  (if (some #(= (keyword migration-table) (first %))
            (analyzer/analyze conn))
    (let [cnt (jdbc/query conn [(str "SELECT COUNT(*) AS count FROM " (name migration-table) " WHERE migration=?") (name mname)])]
      (not (zero? (:count (first cnt)))))
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
    (if (migration-executed? tx (:name migration))
      (do
        ;; 2. Exec rollback
        (exec-stmt tx (:down migration))
        ;; 3. Delete migration from db
        (jdbc/delete! tx migration-table ["migration = ?" (name (:name migration))])
        {(:name migration) true})
      {(:name migration) false})))

(defn migrate
  [conn migrations]
  (let [conn (fix-connection conn)]
    (doall (map (partial migrate-1 conn) (migrations conn)))))

(defn take-until
  "Returns a sequence of successive items from coll until
  (pred item) returns true, including that item. pred must be
  free of side-effects."
  [pred coll]
  (when-let [s (seq coll)]
    (if (pred (first s))
      (cons (first s) nil)
      (cons (first s) (take-until pred (rest s))))))

(defn rollback
  [conn migrations & [do-all?]]
  (let [conn (fix-connection conn)
        rollbacks (reverse (migrations conn))]
    (if do-all?
      (mapv (partial rollback-1 conn) rollbacks)
      (take-until (comp first vals) (map (partial rollback-1 conn) rollbacks)))))

(defn up-to-date?
  "Checks if the database is up to date."
  [conn migrations]
  (let [migration-names
        (map
         (comp name :name)
         (migrations conn))]
    (every? (partial migration-executed? conn) migration-names)))
