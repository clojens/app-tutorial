(ns cors.server
  (:gen-class) ; for -main method in uberjar
  (:require [io.pedestal.service-tools.server :as server]
            [io.pedestal.service.http :as bootstrap]
            [cors.service :as service]
            [io.pedestal.service-tools.dev :as dev]))

(def service-instance
  "Global var to hold service instance."
  nil)

(defn create-server
  "Standalone development/production mode."
  [& [opts]]
  (alter-var-root #'service-instance
                  (constantly (bootstrap/create-server (merge service/service opts)))))

(defn run-dev
  "The entry-point for 'lein run-dev'"
  [& args]
  (dev/init service/service #'service/routes)
  (apply dev/-main args))


(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (let [port (Long/valueOf (first args))]
    (println "Creating server...")
    (create-server [::bootstrap/port port])
    (println (str "Server created. Awaiting connections on port " port))
    (bootstrap/start service-instance)))

;; (-main 8080)
;; (-main 8081)

;; Fns for use with io.pedestal.servlet.ClojureVarServlet

(defn servlet-init [this config]
  (server/init service/service)
  (server/servlet-init this config))

(defn servlet-destroy [this]
  (server/servlet-destroy this))

(defn servlet-service [this servlet-req servlet-resp]
  (server/servlet-service this servlet-req servlet-resp))
