(ns template-server.service
  (:refer-clojure :exclude [+ - * /])
  (:require [clojure.string :as string]
            [io.pedestal.service.http.route.definition :refer [defroutes]]
            [io.pedestal.service.http.body-params :as content-type]
            [io.pedestal.service.http.route :as route]
            [io.pedestal.service.http :as bootstrap]
            [ring.util.response :as ring-resp]
            [net.cgrand.enlive-html :as html]
            [clostache.parser :as mustache]
            [clojure.java.io :as io]
            [plumbing.graph :as graph]
            [plumbing.core :refer [defnk fnk]]
            [garden.core :as garden]
            [garden.def :refer [defrule]]
            [garden.units :as gu :refer [px em]]
            [comb.template :as comb]
            [clojure.string :as str]
            [hiccup.page :as page]
            [hiccup.def :refer [defelem]]
            [template-server.grid :as grid]))

;; Convenient names given context
(def markup list)
(def styles list)

(def normalize (comp #(apply str %) string/capitalize #(string/replace % #"/" "")))
(def not-empty? (comp empty?))


;;;
;;; Garden DSL ~> Cascading Style Sheets (CSS)
;;;

;; Although not required per se, these `defrule` make for more readable code.
;; Also I feel it suites to have essential abstractions/building blocks in plain sight.
(defrule page-body :body)
(defrule headings :h1 :h2 :h3)
(defrule sub-headings :h4 :h5 :h6)
(defrule on-hover :&:hover)
(defrule links :a:link)
(defrule visited-links :a:visited)
(defrule active-links :a:active)
;; These form pretty solid building blocks for further integration into
;; pedestal components. Everything we need is here (discussion in motivation.md)



;; We just bundle styles together in a normal function here, nothing fancy.
;; Could easily become a multimethod/dispatch for several themes/styles on ad-hoc basis.
(defn demo-style
  "Wraps several style rules, allows for easier parsing of final result."
  []
  (styles
   (page-body {:font {:family "Verdana, Sans serif"}
               :background "#f7f7f7 url(/images/noise.png)"})
   (headings {:font {:size (px 20)}})
   (links {:text-decoration :none :color :red}
    (on-hover {:text-decoration :underline :color :blue}))))

;; style rules as response (no file)
(defn style-sheet
  "Ring handler which responds by returning parsed CSS rules."
  [request]
  (-> (ring-resp/response (garden/css (demo-style)))
      (ring-resp/content-type "text/css")))


(defn semantic-grid
  "Demo page of the semantic grid system using garden.
  Compare with the demo on the Semantic Grid website.
  <http://semantic.gs/examples/fixed/fixed.html>"
  [request]
  (ring-resp/response
    (page/html5
     [:head
      [:style (garden/css grid/fixed)]]
     (grid/center
      (grid/top [:h1 "The Semantic Grid System"])
      (grid/main [:h2 "Main"])
      (grid/sidebar [:h2 "Sidebar"])))))

;;;
;;; Method 1) Literal HTML with some Clojure string interpolation
;;;

(defn home-page
  "Returns a body of hypertext markup language as string value.
  Uses some Clojure for 'dynamic' population of a list."
  [request]
  (ring-resp/response
   (format "<html>
           <head><title>Pedestal Template Server</title>
           <!-- Note: this stylesheet is *not* a physical file but will still work since the route and handler
           are hooked up -->
           <link rel='stylesheet' type='text/css' href='/assets/css/main' media='all'>
           <body>%s<br/>%s</body></html>"
           "Each of the links below is rendered by a different templating library. Check them out:"
           (str "<ul>"
                (->> ["hiccup" "enlive" "mustache" "stringtemplate" "comb" "semantic-grid/fixed"]
                     (map #(format "<li><a href='/%s'>%s</a></li>" % %))
                     (str/join ""))
                "</ul>"))))

;;;
;;; Method 2) Hiccup HTML DSL forms with Prismatic Plumbing
;;;

(defnk head
  "Defines a hypertext document head with some defaults."
  [{title "Home | Template Server"}
   {styles "/assets/css/main"} ;<-- not physically hosted
   {keywords ["pedestal" "clojure" "web" "framework" "reactive" "messaging" "clojurescript" "clj" "cljs"]}
   {description "Pedestal is a revolutionary framework for building next-gen internet applications."}]
   [:head
    [:title title]
    [:meta {:http-equiv "content-type" :content "text/html;charset=utf-8"}]
    [:meta {:name "keywords" :content (string/join keywords)}]
    [:meta {:name "description" :content description}]
    (page/include-css styles) ])

(defnk body
  "Defines a default hypertext document body element with some sample default values."
  [{heading "This is the template page for hiccup."}
   {content "Check out the source code for some more remarks"}]
  [:body [:section (markup [:h1 heading]
                           [:article content])]])

(defnk htdoc
  "Generic hypertext document. Takes head & body, returns a Ring response map."
  [head body]
  (ring-resp/response (page/html5 head body)))

(def graph-htdoc
  "The graph itself, minimal setup."
  {:head head
   :meta (fnk [head] head)
   :body body
   :response htdoc})

(def htdoc-eager
  "Compilation strategy."
  (graph/eager-compile graph-htdoc))

(defn hiccup-page
  "The /hiccup page is using hiccup DSL piped through a simple graph, and generate
  a string literal containing the parsed HTML as body of a reponse map.
  <https://github.com/weavejester/hiccup>
  <https://github.com/prismatic/plumbing>"
  [request]
  (->> (into {} (htdoc-eager {:title "Hiccup with Plumbing Graph"}))
       :response))

;;;
;;; Method 3) Enlive templates
;;;

(html/deftemplate enlive-template
                  "public/enlive-template.html"
  [ctxt]
  [:h1] (html/content (:title ctxt))
  [:#the-text] (html/content (:text ctxt))
  [:#the-date] (html/content (:date ctxt)))

(defn enlive-page
  "The /enlive page is done with enlive, plugging in values for title and text.
  source+doc: https://github.com/cgrand/enlive"
  [request]
  (ring-resp/response
   (apply str (enlive-template {:title "Enlive Demo Page"
                                :text "Hello from the Enlive demo page. Have a nice day!"
                                :date (str (java.util.Date.))}))))

;;;
;;; Method 4) {{Mustache}}
;;;

(defn mustache-page
  "The /mustache page is done in (what else?) mustache.
  source+doc: https://github.com/fhd/clostache"
  [request]
  (ring-resp/response
   (mustache/render-resource "public/mustache-template.html"
                             {:title "Mustache Demo Page"
                              :text "Hello from the Mustache demo page. Have a great day!"
                              :date (str (java.util.Date.))})))

;;;
;;; Method 5) $StringTemplate$ (ST4)
;;;

(def template-string
  "<html>
    <body>
      <h1>Hello from {name}</h1>
    </body>
  </html>")

(defn stringtemplate-page
  [request]
  (let [template (org.stringtemplate.v4.ST. template-string \{ \})]
    (ring-resp/response (-> template
                       (.add "name" "String Template")
                       (.render)))))

;;;
;;; Method 6) Comb templates a la <%= erb %>
;;;

(defn comb-page
  "The /comb page is done with the very ERB/JSP-like comb
  templating package. See https://github.com/weavejester/comb"
  [request]
  (ring-resp/response
   (comb/eval (slurp (io/resource "public/comb.html")) {:name "erb"})))


;;;
;;; Routing and service map
;;;

(defroutes routes
  [[["/" {:get home-page} ^:interceptors [bootstrap/html-body]
     ["/assets/css/main" {:get style-sheet}]
     ["/semantic-grid/fixed" {:get semantic-grid}]
     ["/hiccup" {:get hiccup-page}]
     ["/enlive"  {:get enlive-page}]
     ["/mustache"  {:get mustache-page}]
     ["/stringtemplate"  {:get stringtemplate-page}]
     ["/comb" {:get comb-page}]]]])

;; Consumed by template-server.server/create-server
(def service {:env :prod
              ::bootstrap/routes routes
              ::bootstrap/resource-path "/public"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
