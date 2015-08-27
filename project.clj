(defproject migrator "0.1.0-SNAPSHOT"
  :description "Migrator is a SQL migration library written in Clojure"
  :url "https://github.com/azapps/clj-migrator"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jdbc "0.4.1"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [org.clojure/tools.macro "0.1.2"]
                 [analytor/analytor "0.1.0-SNAPSHOT"]]
  :target-path "target/%s"
  :repositories [["snapshots" {:url "https://maven.azapps.de/artifactory/mirakeldb-snapshots"}]
                 ["releases" {:url "https://maven.azapps.de/artifactory/mirakeldb-releases"}]]
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies
                   [[lein-clojars "0.7.0"]
                    [lein-marginalia "0.6.1"]
                    [lein-multi "1.1.0"]
                    [cljss "0.1.1"]
                    [hiccup "0.3.1"]
                    [com.h2database/h2 "1.3.160"]]}})
