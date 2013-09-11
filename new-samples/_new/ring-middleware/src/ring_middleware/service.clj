(ns ring-middleware.service
  (:require [clojure.java.io :as io]
            (hiccup [core :refer :all]
                     [page :refer :all]
                     [form :refer :all])
            [io.pedestal.service.http :as bootstrap]
            [io.pedestal.service.http.route :as route]
            [io.pedestal.service.http.body-params :as body-params]
            [io.pedestal.service.http.route.definition :refer [defroutes]]
            [io.pedestal.service.http.ring-middlewares :as middlewares]
            [io.pedestal.service.interceptor :refer [defhandler definterceptor]]
            [ring.middleware.session.cookie :as cookie]
            [ring.util.response :as ring-resp]))

(defn html-response
  "Wraps HTML received as input and returns a usuable ring response."
  [html]
  (ring-resp/content-type (ring-resp/response html) "text/html"))

;; You can regenerate this .html file using code in the form-generate.clj file
(defhandler intro-form
  "Gather some data from the user to retain in their session.
  Prompt a user for their name, then remember it."
  [req]
  (html-response
   (slurp (io/resource "hello-form.html"))))

(defhandler introduction
  "Place the name provided by the user into their session, then send
  them to hello."
  [req]
  (let [name (-> req :params :name)]
    (-> (ring-resp/redirect "/hello")
        (assoc :session {:name name}))))

;; Behavior dictated by data in the user's session. Using ring
;; middleware means that the request is what gets modified by
;; interceptors.

;; We default to 'Stranger' so users visiting the service can see the
;; behavior of the service when no session data is present.
(defhandler hello
  "Returns the name, if not found defaults to 'Stranger'"
  [req]
  (let [name (or (-> req :session :name)
                 "Stranger")]
    (html-response
     ;; A reponse using hiccup generated HTML tucked in the middle. The head is
     ;; custom function which takes a title to use and contains a default CSS file.
     (html5 (head "Hello, response")
            [:body [:h1 (str "Hello " name "!")]]) )))

;; One of two of the original template generated pages, kept for reference.
(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s" (clojure-version))))

(defn home-page
  "A pure HTML response that uses intern function."
  [request]
  (let [name "HTML"]
  (html-response (str "<html><body><h1>Hello, " name "!</h1></body></html>"))))


;; Two notes:

;; 1: You can create a session interceptor without specifying a store,
;; in which case the interceptor will store the session data nowhere
;; and it will be about as useful as not having it in the first
;; place. Storing session data requires specifying the session store.

;; 2: In this example code we do not specify the secret with which the
;; session data is encrypted prior to being sent back to the
;; browser. This has two consequences, the first being that we need to
;; use the same interceptor instance throughout the service so that the
;; session data is readable and writable to all paths. The second
;; consequence is that session data will become unrecoverable when the
;; server process is ended. Even though the browser retains the
;; cookie, it is not unrecoverable ciphertext and the session
;; interceptor will treat it as non-existant.
(definterceptor session-interceptor
  (middlewares/session {:store (cookie/cookie-store)}))

;; Set up routes to get all the above handlers accessible.
(defroutes routes
  [[["/" {:get intro-form}]
    ["/introduce" ^:interceptors [middlewares/params
                                  middlewares/keyword-params
                                  session-interceptor]
     {:post introduction}]

    ["/hello" ^:interceptors [session-interceptor]
     {:get hello}]

    ["/about" {:get about-page}]
    ["/home" {:get home-page}]
    ]])

;; You can use this fn or a per-request fn via io.pedestal.service.http.route/url-for
;(def url-for (route/url-for-routes routes))

;; Consumed by ring-middleware.server/create-server
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; :bootstrap/interceptors []
              ::bootstrap/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::bootstrap/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::bootstrap/resource-path "/public"

              ;; Either :jetty or :tomcat (see comments in project.clj
              ;; to enable Tomcat)
              ;;::bootstrap/host "localhost"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
