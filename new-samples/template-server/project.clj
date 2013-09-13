; Copyright 2013 Relevance, Inc.

; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
; which can be found in the file epl-v10.html at the root of this distribution.
;
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
;
; You must not remove this notice, or any other, from this software.

(defproject template-server "0.0.1-SNAPSHOT"
  :description "Demonstrates Pedestal page rendering via different template libraries."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [io.pedestal/pedestal.service "0.2.1"]
                 [io.pedestal/pedestal.service-tools "0.2.1"]
                 [io.pedestal/pedestal.jetty "0.1.10"]
                 ;; Remove this line and uncomment the next line to
                 ;; use Tomcat instead of Jetty:
                 [io.pedestal/pedestal.jetty "0.2.1"]
                 ;; [io.pedestal/pedestal.tomcat "0.2.1"]
                 [prismatic/plumbing "0.1.0"]
                 [garden "1.0.0-SNAPSHOT"]
                 [hiccup "1.0.4"]
                 [enlive "1.1.4"]
                 [comb "0.1.0"]
                 [org.antlr/stringtemplate "4.0.2"]
                 [de.ubercode.clostache/clostache "1.3.1"]]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources", "resources/public"]
  :aliases {"run-dev" ["trampoline" "run" "-m" "template-server.server/run-dev"]}
  :repl-options  {:init-ns user
                  :init (try
                          (use 'io.pedestal.service-tools.dev)
                          (require 'template-server.service)
                          ;; Nasty trick to get around being unable to reference non-clojure.core symbols in :init
                          (eval '(init template-server.service/service #'template-server.service/routes))
                          (catch Throwable t
                            (println "ERROR: There was a problem loading io.pedestal.service-tools.dev")
                            (clojure.stacktrace/print-stack-trace t)
                            (println)))
                  :welcome (println "Welcome to pedestal-service! Run (tools-help) to see a list of useful functions.")}
  :main ^{:skip-aot true} template-server.server)
