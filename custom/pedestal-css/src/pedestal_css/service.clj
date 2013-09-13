(ns pedestal-css.service
    (:require [clojure.string :as string]
              [clojure.pprint :refer [pprint]]
              [io.pedestal.service.http :as bootstrap]
              [io.pedestal.service.http.route :as route]
              [io.pedestal.service.http.body-params :as body-params]
              [io.pedestal.service.http.route.definition :refer [defroutes]]
              [ring.util.response :as ring-resp]
              [garden.core :refer [css]]
              [garden.units :refer [px percent pt em]]
              [garden.def :refer [defrule]]
              [hiccup.page :refer [include-css html5]]
              [hiccup.core :refer [html]]
              [hiccup.def :refer [defelem wrap-attrs]]
              [plumbing.core :refer [defnk fnk sum]]
              [plumbing.graph :as graph]))


(def styles list)
(def markup list)

;;;
;;; Components
;;;

(def normalize (comp #(apply str %) string/capitalize #(string/replace % #"/" "")))
(def not-empty? (comp empty?))



;;;
;;; Graph maps
;;;

(def style-graph
  "A graph specifying the same computation as 'stats'"
  {:n  (fnk [rules]   (count rules))
   :m  (fnk [rules n] (/ (sum identity rules) n))
   :m2 (fnk [rules n] (/ (sum #(* % %) rules) n))
   :v  (fnk [m m2]    (- m2 (* m m)))})


(def request-graph
  "A graph helping me learn more about Ring requests."
  {:n  (fnk [req]   (list req))
   :m  (fnk [req n] (list req n))
   :m2 (fnk [req m] (list req m))
   :v  (fnk [m m2]  (list m m2))
   })



;;   {:n  (fnk [xs]   (count xs))
;;    :m  (fnk [xs n] (/ (sum identity xs) n))
;;    :m2 (fnk [xs n] (/ (sum #(* % %) xs) n))
;;    :v  (fnk [m m2] (- m2 (* m m)))})

;; We can "compile" this Graph to produce a single function
(def styles-eager (graph/eager-compile style-graph))
(def request-eager (graph/eager-compile request-graph))

;; We can modify and extend styles-graph using ordinary operations on maps
(def extended-styles
  (graph/eager-compile
    (assoc style-graph
      :sd (fnk [^double v] (Math/sqrt v)))))


;;;
;;; Define CSS/Garden DSL style rules
;;;

(defrule body :body)
(defrule headings :h1 :h2 :h3)
(defrule sub-headings :h4 :h5 :h6)
(defrule on-hover :&:hover)
(defrule links :a:link)
(defrule visited-links :a:visited)
(defrule active-links :a:active)

(defn demo-theme []
  (styles
   (body {:font {:family "Verdana, Sans serif"}})
   (headings {:font {:size (px 20)}})
   (links {:text-decoration :none :color :red}
    (on-hover {:text-decoration :underline :color :blue}))))


;;;
;;; Define HTML/Hiccup DSL elements
;;;

(defelem headline [text] [:h1 text])
(defelem head [title] [:head [:title title] (include-css "/assets/css/main")])
(defelem intro [tagline] [:section [:article [:h1 (normalize tagline)]]])

(defelem htdoc [title tagline text]
  (ring-resp/response  ; defautl response map
   (html5 (head title) ; html5>html
          [:body
           ; some new html5 semantic elements
           [:section
            ;; so the markup here is to wrap those two inside a single list to
            ;; be seen as adjacent siblings
            [:article (markup [:h1 tagline]
                              [:p.content text])]]])))

(defn stylesheet
  "Respond to request by sending a response which is the parsed output of
  garden Clojure DSL forms to a string of CSS. Sets the correct content type."
  [request]
  (-> (ring-resp/response (css (demo-theme)))
      (ring-resp/content-type "text/css")))

(defn request-page
  [request]
  (htdoc "Test page"
         "Generic test/debug page."
         (str "Nothing to see here!" (with-out-str (pprint request)))))

;(into [] (request-eager {:req request}))])))

(defn about-page
  "A somewhat contrived about-page example which demonstrates a simple concept of wrapping
  the generic responses in a `htdoc` function that takes 3 arguments, a title, headline and content."
  [request]
  (htdoc (normalize (:path-info request))
         "Informative page isn't it?"
         (format "Clojure %s" (clojure-version))))

(defn home-page
  [request]
  (htdoc "Home"
         "CSS Service Middleware Demo"
         (markup [:p "For this Pedestal sample service app, no literal CSS, HTML,
                  or any files were used here other than native Clojure."]
                 [:a {:href "/about"} "About"]
                 [:br]
                 [:a {:href "/test"} "Test"]
                 [:br]
                 [:p "The CSS 'file' we used to respond to the HTTP requests, the
                  /assets/css/main path, can be found here:"]
                 [:a {:href "/assets/css/main"} "stylesheet 'main.css'"])))

;; (def my-handler
;;     (routes some-handler
;;             some-other-handler))

(def routes1
  [[["/" {:get home-page}
     ;; Set default interceptors for /about and any other paths under /
     ^:interceptors [(body-params/body-params) bootstrap/html-body]
     ["/about" {:get about-page}]

     ;; of course, we could also just write it to a file and static serve it but that wasnt the idea
     ["/assets/css/main" {:get stylesheet}] ; instead, we parse the dsl->string as response
     ["/test" {:get request-page}]
     ]]])

(def routes2 [[["/foo" {:get home-page}]]])
(def allroutes (concat routes1 routes2))

;; Consumed by pedestal-css.server/create-server
(def service {:env :prod
              ::bootstrap/routes allroutes
              ::bootstrap/resource-path "/public"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
