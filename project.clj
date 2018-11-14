(defproject minreact "0.1.7-alpha6"
  :description "ClojureScript React adapter"
  :url "http://github.com/lgrapenthin/minreact"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :deploy-repositories {"clojars" {:sign-releases false}}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :profiles
  {:dev
   [:repl
    {:dependencies [[org.clojure/clojurescript "1.7.228"]
                    [cljsjs/react "15.1.0-0"]
                    [cljsjs/react-dom "15.1.0-0"]

                    [sablono "0.7.1"]
                    [org.clojure/tools.nrepl "0.2.12"]

                    [com.cemerick/piggieback "0.2.1"]]
     :plugins [[lein-cljsbuild "1.1.2"]
               [lein-figwheel "0.5.0-6"]]
     :clean-targets ^{:protect false} [:target-path "out" "resources/public/cljs"]

     :figwheel {:nrepl-port 7888
                :nrepl-middleware
                ["cemerick.piggieback/wrap-cljs-repl"
                 "cider.nrepl/cider-middleware"]}

     :cljsbuild
     {:builds
      [{:id "dev"
        :source-paths ["src" "dev"]
        :figwheel true
        :compiler {:main dev.core
                   :asset-path "cljs/out"
                   :output-to  "resources/public/cljs/main.js"
                   :output-dir "resources/public/cljs/out"}}]}}]})
