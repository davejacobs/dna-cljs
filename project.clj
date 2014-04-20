(defproject example "0.1.0"
  :description "This is an example"
  :url "http://example.com"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 ; [org.clojure/clojurescript "0.0-2173"]

                 ;; Until the issue mentioned below is fixed, use this version
                 ;; when trying out Austin REPL:
                 ;; https://github.com/cemerick/austin#installation
                 [org.clojure/clojurescript "0.0-2156"]
                 
                 ;; Server, routing
                 [http-kit/http-kit "2.1.16"]
                 [compojure/compojure "1.1.5"]

                 ;; DOM manipulation
                 [domina "1.0.2"]

                 ;; Async channels
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]

                 ;; Websockets + core.async
                 [jarohen/chord "0.3.1"]

                 ;; Middleware
                 [ring/ring-devel "1.1.8"]
                 [ring/ring-json "0.2.0"]]

  :plugins [[lein-cljsbuild "1.0.2"]]
  :aliases {"serve" ["run" "-m" "server.handler"]}

  ;; To use the Austin REPL:
  :profiles {:dev {:repl-options {:init-ns client.app}
                   :plugins [[com.cemerick/austin "0.1.4"]]}}

  :source-paths ["src" "src-cljs"]
  :resource-paths ["resources"]

  :cljsbuild { 
    :builds [{:source-paths ["src-cljs"]
              :compiler {
                :output-to "resources/public/javascripts/lib.js"
                :output-dir "resources/public/javascripts"
                :optimizations :whitespace
                :source-map "resources/public/javascripts/lib.js.map"}}]})
