(defproject mireact "0.1.0-SNAPSHOT"
  :description "ClojureScript React adapter"
  :url "http://github.com/lgrapenthin/mireact"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :profiles
  {:dev
   {:dependencies [[org.clojure/clojurescript "0.0-3308"]
                   [cljsjs/react "0.13.3-0"]]
    :plugins [[lein-cljsbuild "1.0.6"]
              [lein-figwheel "0.3.5"]]
                   
    :figwheel {:nrepl-port 7888}
                   
    :cljsbuild
    {:builds
     [{:id "dev"
       :source-paths ["src" "dev"]
       :figwheel true
       :compiler {:main dev.core}}]}}})
