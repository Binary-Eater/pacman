(defproject pacman "0.1.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clojure-lanterna "0.9.7"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [ubergraph "0.4.0"]]
  :main ^:skip-aot pacman.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
