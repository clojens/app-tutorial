(ns cors.service
    (:require [clojure.java.io :as io]
              [io.pedestal.service.log :as log]
              [io.pedestal.service.http :as bootstrap]
              [io.pedestal.service.http.route :as route]
              [io.pedestal.service.http.body-params :as body-params]
              [io.pedestal.service.http.route.definition
               :refer [defroutes]]
              [io.pedestal.service.interceptor
               :refer [defhandler defbefore defafter definterceptor]]
              [io.pedestal.service.http.sse
               :refer [sse-setup send-event end-event-stream]]
              [io.pedestal.service.http.impl.servlet-interceptor
               :as servlet-interceptor]
              [ring.util.response :as ring-response]))

(defn send-thread-id [context]
  (send-event context "thread-id" (str (.getId (Thread/currentThread)))))

(defn thread-id-sender [{{^ServletResponse response :servlet-response
                 :as request} :request :as context}]

  (log/info :msg "starting sending thread id")
  (dotimes [_ 10]
    (Thread/sleep 3000)
    (send-thread-id context))
  (log/info :msg "stopping sending thread id")

  (end-event-stream context))


(defhandler send-js
  "Send the client a response containing the stub JS which consumes an
  event source."
  [req]
  (log/info :msg "returning js")
  (-> (ring-response/response (slurp (io/resource "blob.html")))
      (ring-response/content-type "text/html")))

(definterceptor thread-id-sender (sse-setup thread-id-sender))

(defroutes routes
  [[["/js" {:get send-js}]
    ["/" {:get thread-id-sender}]]])

;; You can use this fn or a per-request fn via io.pedestal.service.http.route/url-for
;(def url-for (route/url-for-routes routes))

;; Consumed by cors.server/create-server
(def service {:env :prod

              ::bootstrap/routes routes

              ::bootstrap/allowed-origins ["http://localhost:8080"]

              ::bootstrap/resource-path "/public"

              ;;::bootstrap/host "localhost"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
