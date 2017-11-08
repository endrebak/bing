
(defproject bing "0.1.0-SNAPSHOT"
  :description "Fetch gene aliases from mygene.info."
  :url "http://github.com/biocore-ntnu/bing"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.7.0"],
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/tools.cli "0.3.5"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]} :uberjar {:aot :all}}
  :plugins [[lein-midje "3.2.1"]]
  :main bing.core)
