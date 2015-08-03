(defproject minreact "0.1.0-SNAPSHOT"
  :description "ClojureScript React adapter"
  :url "http://github.com/lgrapenthin/minreact"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :profiles
  {:dev
   {:dependencies [[org.clojure/clojurescript "1.7.28"]
                   [cljsjs/react "0.13.3-0"]]
    :plugins [[lein-cljsbuild "1.0.6"]
              [lein-figwheel "0.3.7"]]
    
    :figwheel {:nrepl-port 7888}
    
    :cljsbuild
    {:builds
     [{:id "dev"
       :source-paths ["src" "dev"]
       :figwheel true
       :compiler {:main dev.core}}]}}})
