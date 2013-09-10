(ns cors.service-test
  (:require [clojure.test :refer :all]
            [clojure.java.shell :refer [sh]]
            [io.pedestal.service.test :refer :all]
            [io.pedestal.service.http :as bootstrap]
            [cors.service :as service]
            ;[io.pedestal.service-tools.server :as server]
            [cors.server :refer [run-dev service-instance
                                 create-server]]
            ))

;; (def service
;;   (::bootstrap/service-fn (bootstrap/create-servlet service/service)))

;; (::bootstrap/stop-fn x)

;; (defn prepare-test []
;;   (try
;;     (spit "/root/resources/public/abc.txt" "pedestal-test")
;;     (catch java.io.FileNotFoundException e (list e))))

;; (first (prepare-test))

;; (deftest test-dev-server
;;   (testing "if the public folder")

;; (test-dev-server)

;; ;bootstrap/stop
;; (defonce x (run-dev 8080))
;; (:body (response-for service :get "/abc.txt"))
;; (:body (response-for service :get "/js"))
;; (:body (response-for service :get "/eventsource.js"))
;; (:headers (response-for service :get "/eventsource.js"))

;; ;(:body (response-for service :get "/js"))
;; ; (server/create-server [::bootstrap/port (Long/valueOf 8081)])
;; ;; (server/create-server [::bootstrap/port (Long/valueOf 8080)])
;; ;(bootstrap/start service-instance)
;; ;(about-page-test)
