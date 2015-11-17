(defproject minreact "0.1.4"
  :description "ClojureScript React adapter"
  :url "http://github.com/lgrapenthin/minreact"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cljsjs/react "0.13.3-1"]]
  :profiles
  {:dev
   {:dependencies [[org.clojure/clojurescript "1.7.122"]
                   
                   [sablono "0.3.6"]]
    :plugins [[lein-cljsbuild "1.1.0"]
              [lein-figwheel "0.3.9"]]
    :clean-targets ^{:protect false} [:target-path "out" "resources/public/cljs"]
    
    :figwheel {:nrepl-port 7888}
    
    :cljsbuild
    {:builds
     [{:id "dev"
       :source-paths ["src" "dev"]
       :figwheel true
       :compiler {:main dev.core
                  :asset-path "cljs/out"
                  :output-to  "resources/public/cljs/main.js"
                  :output-dir "resources/public/cljs/out"}}]}}})
