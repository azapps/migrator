(defproject migrator "0.1.4"
  :description "A library to handle SQL migrations"
  :url "https://github.com/azapps/migrator"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jdbc "0.4.1"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [org.clojure/tools.macro "0.1.2"]
                 [analytor/analytor "0.1.1"]])
