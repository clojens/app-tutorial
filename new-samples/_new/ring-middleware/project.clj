; Copyright 2013 Relevance, Inc.

; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
; which can be found in the file epl-v10.html at the root of this distribution.
;
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
;
; You must not remove this notice, or any other, from this software.

(defproject ring-middleware "0.0.1-SNAPSHOT"
  :description "A Pedestal and ring middleware sample service application."
  :url "http://pedestal.io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [io.pedestal/pedestal.service "0.2.1"]
                 [io.pedestal/pedestal.service-tools "0.2.1"]
                 [hiccup "1.0.4"]
                 [garden "1.0.0-SNAPSHOT"]
                 [swiss-arrows "0.6.0"]
                 [net.sf.jtidy/jtidy "r938"]
                 ;; Remove this line and uncomment the next line to
                 ;; use Tomcat instead of Jetty:
                 [io.pedestal/pedestal.jetty "0.2.1"]
                 ;; [io.pedestal/pedestal.tomcat "0.2.1"]
                 ]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources", "resources/public"]
  :aliases {"run-dev" ["trampoline" "run" "-m" "ring-middleware.server/run-dev"]}
  :repl-options  {:init-ns user
                  :init (try
                          (use 'io.pedestal.service-tools.dev)
                          (require 'ring-middleware.service)
                          ;; Nasty trick to get around being unable to reference non-clojure.core symbols in :init
                          (eval '(init ring-middleware.service/service #'ring-middleware.service/routes))
                          (catch Throwable t
                            (println "ERROR: There was a problem loading io.pedestal.service-tools.dev")
                            (clojure.stacktrace/print-stack-trace t)
                            (println)))
                  :welcome (println "Welcome to pedestal-service! Run (tools-help) to see a list of useful functions.")}
  :main ^{:skip-aot true} ring-middleware.server)
