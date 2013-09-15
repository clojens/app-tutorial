(ns

  ^{:doc "Template polyglot sample offers example uses of: Hiccup and Enlive,
    Markdown, StringTemplate (ST4), Comb (ERB/JSP-like) Clostache, Garden CSS,
    Clostache and pure HTML strings sent as response maps (in a few cases through
    computation of Pristmatic plumbing graph keyword functions) using Pedestal,
    routing, over Ring to a Jetty server instance. All the 'web pages' generated
    in this sample are directly usable and viewable by mere evaluation of the
    (server/run-dev) expression in `server.clj`."

    :author "Copyright 2013 Relevance, Inc."

    :license "The use and distribution terms for this software are covered by the
    Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
    which can be found in the file epl-v10.html at the root of this distribution.

    By using this software in any fashion, you are agreeing to be bound by
    the terms of this license.

    You must not remove this notice, or any other, from this software."}

  template-server.service

  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [io.pedestal.service.http.route.definition :refer [defroutes]]
            [io.pedestal.service.http.body-params :as content-type]
            [io.pedestal.service.http.route :as route]
            [io.pedestal.service.http :as bootstrap]
            [ring.util.response :as ring-resp]
            [net.cgrand.enlive-html :as html]
            [template-server.grid :as grid]
            [clostache.parser :as mustache]
            [markdown.core :as markdown]
            [comb.template :as comb]
            [plumbing.graph :as graph]
            [plumbing.core :refer [defnk fnk]]
            [garden.core :refer [css]]
            [garden.units :as gu :refer [px em]]
            [garden.def :refer [defrule]]
            [hiccup.page :as page]
            [hiccup.def :refer [defelem]])
  (:import [org.w3c.tidy Tidy]
           [java.io StringReader StringWriter]))


;[cfg.current :as cfg]


;; Convenient names
(def markup list)
(def styles list)
(def md->html markdown/md-to-html-string)
(def normalize (comp #(apply str %) string/capitalize #(string/replace % #"/" "")))
(def not-empty? (comp empty?))

;; Tidy
(defn jtidy []
  (doto (new Tidy)
    (.setDocType "html PUBLIC")
    (.setXmlTags true)
    (.setSmartIndent true)
    (.setWraplen 120)))

(defn format-html [html]
  (let [w (StringWriter.)]
    (.parse (jtidy) (StringReader. (str html)) w)
    (str ;"<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
         w)))

;;;
;;; Garden DSL ~> Cascading Style Sheets (CSS)
;;;

;; Although not required per se, these `defrule` make for more readable code.
;; Also having essential abstractions/building blocks in plain sight isn't bad.
(defrule page-body :body)
(defrule headings :h1 :h2 :h3)
(defrule sub-headings :h4 :h5 :h6)
(defrule on-hover :&:hover)
(defrule links :a:link)
(defrule visited-links :a:visited)
(defrule active-links :a:active)


(defn demo-style
  "Wraps several style rules, allows for easier parsing of final result. Reusable
  blocks (e.g. for a theme) could be composed this way."
  []
  (styles
   (page-body {:font {:family "Verdana, Sans serif"}
               :background "#f7f7f7 url(/images/noise.png)"})
   (headings {:font {:size (px 20)}})
   (links {:text-decoration :none :color :red}
    (on-hover {:text-decoration :underline :color :blue}))))

(defn style-sheet
  "Ring handler which responds by returning parsed CSS rules.
  This would constitute the use of a concept known as server-side CSS."
  [request]
    (-> (ring-resp/response
         (garden/css {:output-layout :expanded} (demo-style)))
        (ring-resp/content-type "text/css")))

(defn semantic-grid
  "Demo page of the semantic grid system using garden, compare with the demo on
  the Semantic Grid website. <http://semantic.gs/examples/fixed/fixed.html>"
  [request]
  (let [layout (->> request :params :type)]
    (ring-resp/response
     (page/html5
      [:head
       ;; Use some string interpolation to get part of the request params.
       [:title (format "Demo %s" layout)]
       ;; Instead of parsing the CSS string to be served up as a ring response
       ;; and referenced through `include-css`, we use a internal stylesheet here.
       ;; This saves us 1 HTTP GET request on the client side. This CSS is just as
       ;; reusable as external style sheets since we take the server-side approach
       ;; here and have the power of Clojure at our hands. The cascading nature
       ;; of CSS would give presedence over external ones, something to keep in mind.
       [:style (garden/css (->> {} grid/grid-eager :grid-layout))]] ;</head>

      [:body
       (grid/center
        (grid/top [:h1 (format "The Semantic Grid System (%s example)" layout)])
        (grid/main [:h2 "Main"])
        (grid/sidebar [:h2 "Sidebar"]))]))))


;;;
;;; Method 1) Literal HTML with some Clojure string interpolation
;;;

(defn home-page
  "Returns a body of hypertext markup language as string value as ring response map."
  [request]
  (ring-resp/response
   (format "<html>
           <head><title>Pedestal Template Server</title>
           <link rel='stylesheet' type='text/css' href='/assets/styles/main.css' media='all'>
           <body>%s<br/>%s</body></html>"
           "Each of the links below is rendered by a different templating library. Check them out:"
           (str "<ul>"
                (->> ["hiccup" "enlive" "mustache" "stringtemplate" "comb" "grid?type=fixed"
                      "markdown" "assets/styles/main.css"]
                     (map #(format "<li><a href='/%s'>%s</a></li>" % %))
                     (string/join ""))
                "</ul>"))))

;;;
;;; Method 2) Hiccup [:html [:dsl "forms"]]
;;;

;; We use prismatic plumbin to cleanly seperate the components.
(defnk head
  "Defines a hypertext document head with some defaults."
  [{title "Home | Template Server"}
   {styles "/assets/styles/main.css"}
   {keywords ["pedestal" "clojure" "web" "framework" "reactive"
              "messaging" "clojurescript" "clj" "cljs" "templates" "samples"]}
   {description "Pedestal is a revolutionary framework for building next-gen
    web applications built by Relevance using Clojure."}]
   [:head
    [:title title]
    [:meta {:http-equiv "content-type" :content "text/html;charset=utf-8"}]
    [:meta {:name "keywords" :content (string/join ", " keywords)}]
    [:meta {:name "description" :content description}]
    (page/include-css styles) ; >= 1 HTTP GET call
    ])

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
  "The graph itself, minimal setup because we've expedited every node function to
  the externally defined `defnk` function bodies however, the arguments are still
  passed through properly thanks to our compiling of this graph."
  {:head head
   :meta (fnk [head] head)
   :body body
   :response htdoc})

(def htdoc-eager
  "Compilation strategy: compile everything eagerly, as opposed to parallel or lazily.
  <https://github.com/prismatic/plumbing>"
  (graph/eager-compile graph-htdoc))

(defn hiccup-page
  "The /hiccup page is using hiccup DSL piped through a simple graph, and generate
  a string literal containing the parsed HTML as body of a reponse map.
  <https://github.com/weavejester/hiccup>"
  [request]
  ;; Since none of the nodes require arguments explicitely, we're safe to construct it
  ;; without providing any arguments ourselves (although possible, see below).
  (->> (into {} (htdoc-eager {:title "Hiccup with Plumbing Graph"}))
       :response))

;;;
;;; Method 3) [:#enlive-templates]
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
  "The /mustache page is done in (what else?) mustache, known for
  its {{handlebars}} notation. <https://github.com/fhd/clostache>"
  [request]
  (ring-resp/response
   (mustache/render-resource "public/mustache-template.html"
                             {:title "Mustache Demo Page"
                              :text "Hello from the Mustache demo page. Have a great day!"
                              :date (str (java.util.Date.))})))

;;;
;;; Method 5) String$Template (ST4)
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
;;; Method 6) Comb <%= templates %>
;;;

(defn comb-page
  "The /comb page is done with the very ERB/JSP-like comb
  templating package. See https://github.com/weavejester/comb"
  [request]
  (ring-resp/response
   (comb/eval (slurp (io/resource "public/comb.html")) {:name "erb"})))


;;;
;;; Method 7) Use Clojure Markdown together with hiccup as article templates
;;;

(def main-content
"# Hello beautiful world!

## A small essay on the wonderous world of Clojure

#### By [the intern](http://example.com)

Welcome friend.

*Please note this is a sample, expand as you see fit*

~~feed the dog~~

    Hello

**done**

a^2 + b^2 = c^2
")


(defn markdown-page
  "Here we use Markdown to illustrate a common web development paradigm often
  found in the wild, in boilerplates, templates, MVC frameworks and such: the
  clear seperation of content from structure (earlier we took out style from
  structure and content). This is a Utopia, more so for practical reasons:
  the # and ## in the Markdown are strictly structural, outline components.
  A true seperation of content from structure may be more easily achieved
  by something like in the clostache example by sending the data object along
  that would carry the content."
  [request]
  (ring-resp/response
   ;; Make this sample feel special by having pretty-printed output.
   (format-html
    (page/html5
     [:head
      [:title "Markdown Templates"]]
     [:body
      [:section
       [:article
        ;; Make this example feel special, pretty print the HTML
        (md->html main-content)]]]))))

;(println (->> {} markdown-page :body))

;;;
;;; Routing and service map
;;;

;; Final things to do is to manually setup the routes and then to define
;; a service map that can be consumed by template-server.server/create-server

(defroutes routes
  [[["/" {:get home-page}
     ^:interceptors [bootstrap/html-body]
     ["/assets/styles/main.css" {:get style-sheet}]
     ["/grid" {:get semantic-grid}]
     ["/hiccup" {:get hiccup-page}]
     ["/enlive"  {:get enlive-page}]
     ["/mustache"  {:get mustache-page}]
     ["/stringtemplate"  {:get stringtemplate-page}]
     ["/comb" {:get comb-page}]
     ["/markdown" {:get markdown-page}]
     ]]])

(def service {:env :prod
              ::bootstrap/routes routes
              ;; Hosted/shared files under ./resources/public/ as doc root
              ::bootstrap/resource-path "/public"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
