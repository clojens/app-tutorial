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

;; TODO: These form pretty solid building blocks for further integration into
;; pedestal components. Everything we need is here. For one I imagine it is easiest
;; to access CSS styles based on a path that can be automagically generates. The
;; path can influince the generated CSS because we can extract the query properties
;; from the request, we can use that easily for benefit of e.g. a certain theme.
;; http://localhost:8080/assets/css/main?theme=basic ~> ring ~> dispatch ~> graph <- css-gen
;; Given that one such path wouldn't change too often, the browser caching would work
;; like normal and this method shouldn't pose a bottleneck. One could always just serve
;; static file content through means of either reading or sharing but what fun is that?

;; Now one problem would be that when you would change the CSS, it would be additionally
;; extra HTTP calls to GET it, and thats why many developers cram everything in 1 single
;; CSS file that is concatenated (optimized) or not but often generic enough to be able
;; and use it in a single site perhaps even with different styles for subdomains.
;; Often this boils down to how often the CSS changes. Highly modern frontier / fashion
;; oriented sites would change as fashion changes or their subcultures ideal does.

;; In fact, its probably better to speak of a single monolithic CSS file in those cases,
;; but many sites e.g. those that have demo code would use multiple files to seperate those
;; concerns for the author/maintainer of those demonstrations. Other sites use different
;; <link> elements in the head to reference external files that have theme based styling
;; and so on.

;; Of course, most of this could also be done on the client side of things:
;; http://www.javascriptkit.com/javatutors/loadjavascriptcss.shtml
;; Which relies on client side DOM manipulation through JavaScript in the browser and
;; adds new <link> elements to the <head> node, or removes others.

;; But since I've been out of this game for long, and only know Pedestal for a bit, I'm not
;; entirely sure about some other things. Essentially, Pedestal also is about DOM manipulation
;; be it through some sophisticated means of messaging vectors and decoupling the main
;; parts of what make up abstract concepts like 'a web site' into what engineers really were
;; looking for in all those cases they found their creations (when scaled and advanced)
;; become a terrible mess (the soup).

;; So I feel we could essentially do, if you'd like, it in a very similiar manner with styles.
;; There is a big difference to the front-end though. You'll notice it once you venture in
;; domains of Google Closure Library - basically it means modify or die. Either you follow
;; their rules, or choke that the JS won't work in compiled form since you are missing the
;; references. One advantage HTML has always had, was that it facilitated cargo-cult coding
;; (copy/paste, no thinking required). Since that doesn't seem to fit our potential audience
;; too much, I'll leave this point to be for now. We've basically given that up when using
;; hiccup over HTML, garden over CSS doesn't change that.

;; I have to shift my thoughts to try and place myself in my developer role again though.
;; Is this something I would seek from a RAD perspective? Copy/paste (with full understanding)
;; does save us a lot of time and it might not be something we need to do as original work
;; every time. It really begs for widget-like structures to wrap and contain HTML/CSS/JS
;; and be able to have them work as tags placeable on the big board that is a page. Moreover
;; I've become convinced (more dogmatic I guess) that there *is* a difference in the CSS grid
;; approach. Mere simple fact: I have to use less markup if I use the semantic.gs grid although,
;; I must admit, Twitter Bootstrap did try to clean it up a bit more by doing
;; http://getbootstrap.com/css/#grid-less

;; https://github.com/component/component did address part of this problem as well, although
;; now I am more inclined to feel a bit different when looking at it from the Pedestal perspective.
;; This is simple: component is motivated by universal interoperability. HTML/CSS/JS. We just
;; concluded this might not necessarily be the right approach for Clojure. We use ClojureScript
;; instead to generate JS, by extension we do the same to HTML/CSS when we use Garden/Hiccup.

;; How does Pedestal fit in all this? Well, what Pedestal does is that it transforms 'the DOM'
;; based on messages sent to components. The tools you use, are files (JS/HTML but not CSS)
;; generated by the tools components, but what the developer would use for HTML are templates
;; with often a single point of dynamic entry (the edges) and a regular static CSS file served
;; through a public folder.

;; Perhaps a next logical step would be to control the construction of widgets, through dynamic
;; input in a factory perhaps, and allow for those to be modified by messages. These widgets in turn
;; would prove very much copy-paste friendly and can be bundled for reuse within Pedestal.

;; With that in place, you could do the whole round trip, with or without a service to generate
;; the templates, the messages could do that and you'd have a generic template-service in the
;; air to listen and respond to MESSAGES, instead of raw code like I did here. The CSS sheet is,
;; after all, just another tree we can play around with freely.









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
