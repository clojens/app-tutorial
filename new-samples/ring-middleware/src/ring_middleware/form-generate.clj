(ns
  ^{:doc "Small demonstration of a few form/hiccup tricks used to
    generate the form .html file (already present) in a dynamic
    manner. Bottom of this file contains a commented example on
    how to call the function to generate a new form with a list
    of text fields as provided."}

  ring-middleware.form-generate

   (:require [clojure.string :as string]
             (hiccup [core :refer :all]
                     [page :refer :all]
                     [form :refer :all]))
  (:import [java.io File StringWriter StringReader]
           [org.w3c.tidy Tidy]))

(defn head
  "Generic HTML page head with a small external style sheet."
  [title] [:head [:title title] (include-css "main.css")])

(defn form-elements
  "Abstraction of HTML form elements by use of Hiccup DSL higher-order
  functions for control element creation mapping over a sequence."
  [items]
  (letfn [(required? [x] (->> x name str last (= \!)))
          (drop-exclamation [x] (->> x name str drop-last string/join))
          (normalize [x] (if (required? x) (drop-exclamation x)
                           (if (keyword? x) (->> x name str) x)))
          (capitalize [x] (string/capitalize (normalize x)))]
    ;; map over sequence and take normalize/required attributes and values
    (let [norm-items (map #(list (label (normalize %) (capitalize %))
                                 (text-field {:required (str (required? %))} (normalize %))
                                 [:br])
                          items)]
      (with-group "session" (list norm-items)))))

(defn tidy
  "Takes a string of old-html and 'tidies' it up using JTidy.
  Returns the new, tidied html."
  [old-html]
  (let [new-html (doto (Tidy.)
                   (.setSmartIndent true)
                   (.setShowWarnings true)
                   (.setQuiet false)
                   (.setTidyMark false))
        writer (StringWriter.)
        reader (StringReader. old-html)]
    (.parse new-html reader writer)
    (str writer)))

(defn write-form!
  "Writes the HTML to the static file."
  [path title items]
  (if-let [markup (html5
                   (head title)
                   [:body
                    [:h1 "Enter your name and I will remember you."]
                    (form-to [:post "/introduce"]
                             (list
                              (form-elements items)
                              (submit-button "submit")))
                    [:p "Alternatively, visit the <a href='/hello'>greeting page</a> to see who I think you are."]])]
    (try
      (io!
       (spit (str "./src/" path) (tidy markup))
       :ok))))

;; Finally run this to generate. The sequence may contain symbols, strings or keywords
;; all of them get translated to strings automatically.
(comment
  (write-form! "hello-form.html"
             "Pedestal + Ring Middleware Session/cookie-based form"
             [:name! :sponsor :ticketid])
)
