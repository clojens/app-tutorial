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
            [hiccup.def :refer [defelem]]
            [cfg.current :as cfg])
  (:import [org.w3c.tidy Tidy]
           [java.io StringReader StringWriter]))

;; Convenience
(def markup list)
(def styles list)
(def md->html markdown/md-to-html-string)
(def normalized (comp string/capitalize #(string/replace % #"-" " ")))
(def not-empty? (comp empty?))
(def project-name (normalized (:group @cfg/project)))

;;;
;;; JTIDY
;;;

(defn jtidy []
  (doto (new Tidy)
    (.setDocType "html PUBLIC")
    (.setShowWarnings true)
    (.setTidyMark false)
    ;(.setXmlTags true)
    (.setSmartIndent true)
    (.setWraplen 120)))

(defn format-html
  [html]
  (let [w (StringWriter.)]
    (.parse (jtidy) (StringReader. (str html)) w)
    (str w)))

;;;
;;; GARDEN
;;;

;; Although not required per se, these `defrule` make for more readable styles.
;; Also having essential abstractions/building blocks in plain sight isn't bad.
(defrule page-body :body)
(defrule headings :h1 :h2 :h3)
(defrule sub-headings :h4 :h5 :h6)
(defrule on-hover :&:hover)
(defrule links :a:link)
(defrule visited-links :a:visited)
(defrule active-links :a:active)
(defrule unordered-list :ul)

(defn demo-style
  "Wraps several style rules, allows for easier parsing of final result. Reusable
  blocks (e.g. for a theme) could be composed this way."
  []
  (styles
   (page-body {:font {:family "'Latin Modern Roman', Georgia, 'Times New Roman', Times, serif"}
               :background "#f7f7f7 url(/images/noise.png)"})
   (headings {:font {:size (px 20)}})
   (unordered-list {:list-style :none})
   (links {:text-decoration :none
           :color "#2ba6cb"
           :line-height (em 2.5)
           :margin (px 5) :padding (px 10)}
    (on-hover {:text-decoration :none
               :color :white
               :background "#2ba6cb"
               :border-radius (px 5)}))))

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
;;; LITERAL HTML W/ STRING INTERPOLATION
;;;

(def html-page
  (format "<html>
          <head><title>Pedestal Template Server</title>
          <link rel='stylesheet' type='text/css' href='/assets/styles/main.css' media='all'>
          <body>%s<br/>%s</body></html>"

          "Each of the links below is rendered by a different templating library.
          <p>Check them out below:</p>"

          (str "<ul>"
               (->> ["hiccup" "enlive" "mustache" "stringtemplate" "comb" "grid?type=fixed"
                     "markdown" "assets/styles/main.css"]
                    (map #(format "<li><a href='/%s'>%s</a></li>" % %))
                    (string/join "")) "</ul>")))

(defn home-page
  "Returns a body of hypertext markup language as string value as ring response map."
  [request]
  ;; JTidy cleans and pretty-prints the HTML, use e.g. `curl localhost:8080`
  ;; at your virtual terminal to view the raw source.
  (ring-resp/response (format-html html-page)))

;;;
;;; HICCUP
;;;

;; We use prismatic plumbing to succinctly define components of hiccup vectors
;; using sane defaults which can be modified at will. The DSL is piped through
;; a simple graph, resulting in HTML sent as a Ring response.
;; <https://github.com/weavejester/hiccup>

;; Define a hypertext document head, body and finally document with a few nested
;; structural elements (markup) and attributes.

(defnk head
  [{title (str "Home | " project-name)}
   {styles "/assets/styles/main.css"}
   {keywords ["pedestal" "clojure" "web" "framework" "reactive"
              "messaging" "clojurescript" "clj" "cljs" "templates" "samples"]}
   {description "Describe your website."}]
   [:head
    [:title title]
    [:meta {:http-equiv "content-type" :content "text/html;charset=utf-8"}]
    [:meta {:name "keywords" :content (string/join ", " keywords)}]
    [:meta {:name "description" :content description}]
    (page/include-css styles) ; >= 1 HTTP GET call
    ])

(defnk body
  [{heading "This is the template page for hiccup."}
   {content "Check out the source code for some more remarks"}]
  [:body [:section (markup [:h1 heading]
                           [:article content])]])

(defnk htdoc
  "Generic hypertext document as ring response body compiled from hiccup."
  [head body]
  (ring-resp/response (page/html5 head body)))

(def graph-htdoc
  "Define the plumbing graph which we'll pipe the forms through and compute
  the final document result from defnks. <https://github.com/prismatic/plumbing>"
  {:head head :body body :response htdoc})

(def htdoc-eager
  "Compilation strategy: compile everything eagerly, as opposed to parallel or lazily."
  (graph/eager-compile graph-htdoc))

;; Since we had `htdoc` compute the response map, we don't need it here.
(defn hiccup-page
  [request]
  (->> {:description "Pedestal is a revolutionary framework for building
        next-gen web applications built by Relevance using Clojure."}
       htdoc-eager :response))

;;;
;;; ENLIVE
;;;

(html/deftemplate enlive-template
                  "public/enlive-template.html"
  [ctxt]
  [:h1] (html/content (:title ctxt))
  [:#the-text] (html/content (:text ctxt))
  [:#the-date] (html/content (:date ctxt)))

(defn enlive-page
  "The /enlive page is done with enlive, plugging in values for title and text.
  <https://github.com/cgrand/enlive>"
  [request]
  (ring-resp/response
   (apply str (enlive-template {:title "Enlive Demo Page"
                                :text "Hello from the Enlive demo page. Have a nice day!"
                                :date (str (java.util.Date.))}))))

;;;
;;; MUSTACHE
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
;;; STRINGTEMPLATE
;;;

;; StringTemplate is a java template engine for generating source code,
;; web pages, emails, or any other formatted text output. StringTemplate
;; is particularly good at code generators, multiple site skins, and
;; internationalization / localization. StringTemplate also powers ANTLR.

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
;;; COMB
;;;

;; Comb is a simple templating system for Clojure. Usable to embed fragments of
;; Clojure code into text files. The <% %> tags embed a section of code with
;; side-effects while the <%= %> tags will be subsituted for the value of
;; the expression within them.
;; <https://github.com/weavejester/comb>

(defn comb-page
  [request]
  (ring-resp/response
   (comb/eval (slurp (io/resource "public/comb.html")) {:name "erb"})))


;;; MARKDOWN
;;;

(defn markdown-page
  "Use Markdown to illustrate a web development paradigm, the notion there
  is a distinct difference between structural markup and what is normally
  conceived as content, in this case the text for a article. True separation
  is often not without a cost, much more painful than separating out the styles.
  There are web sites who pull *all* content, sometimes even *all* markup as well,
  from databases. However, a much cleaner solution is to allow at least some
  structure to seep through, be it less obtrusive (markdown)."
  [request]
  (ring-resp/response
    (page/html5
     [:head
      [:title "Markdown Templates"]]
     [:body
      [:section
       [:article
        ;; Make this example feel special, pretty print the HTML
        ;(md->html "# A very short article")
        ;; Since clj-markdown doesn't have a function for file-in string-out
        (md->html (slurp "./resources/public/markdown.md"))]]])))


;; Routing and service map

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
