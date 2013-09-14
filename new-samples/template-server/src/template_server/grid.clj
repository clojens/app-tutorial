(ns template-server.grid
  "Demonstration using Garden to recreate the Semantic Grid framework.
  Thanks to Joel Holbrook (noprompt) for the work on writing this up.

  * <https://github.com/noprompt/garden>
  * <http://semantic.gs/>
  * <https://github.com/twigkit/semantic.gs/blob/master/stylesheets/scss/grid.scss>"

  (:refer-clojure :exclude [+ - * /])
  (:require [garden.core :refer [css]]
            [garden.units :refer [px em percent]]
            [garden.color :refer [hsl]]
            [garden.arithmetic :refer [+ - * /]]
            [garden.def :refer [defrule]]
            [plumbing.graph :as graph]
            [plumbing.core :refer [defnk fnk]]))

;; Alias `styles` (like in hiccup, garden adjacent siblings
;; should be wrapped in a list to ensure proper parsing)
(def styles list)

;; A garden mixin
(def center-text {:text-align "center"})

;; A few garden rule functions to make the code more expressive and readable.
(defrule center :div.center)
(defrule top :section#top)
(defrule main :section#main)
(defrule sidebar :section#sidebar)
(defrule headings :h1 :h2 :h3)

(def clearfix
  ["&" {:*zoom 1}
   ["&:before" "&:after" {:content "\"\"" :display "table"}]
   ["&:after" {:clear "both"}]])

;; --------------
;; Plumbing Graph
;; --------------
;;
;; A different destructuring syntax than 'fn' and 'defn', which automatically
;; infer input and output schemata. For more information see the README at:
;; <https://github.com/Prismatic/plumbing/tree/master/src/plumbing/fnk>

(def grid-graph
  "Compute the grid (graph) given the defaults, pipe through the anonymous
  functions and calculate the final grid metrics."
  {

   ;; Default values for our essential properties to be able and compute
   ;; grid metrics. Making these a node in the graph allows us to use their
   ;; symbols more freely here. Also they allow for future logic to be
   ;; applied to these input values e.g. max number of columns, column minimal
   ;; width and so on. It might also make a good place should additional unit
   ;; arithmetics need to be applied (for media-queries etc.)
   :columns      (fnk [{grid-columns 12}] grid-columns)
   :column-width (fnk [{grid-column-width (px 60)}] grid-column-width)
   :gutter-width (fnk [{grid-gutter-width (px 20)}] grid-gutter-width)
   :total-width  (fnk [{grid-total-width  (px 960)}] grid-total-width)

   ;:|m| (fnk [>>>] {:>>> >>>})

   :grid-width (fnk [column-width gutter-width columns]
                    (+ (* column-width columns)
                       (* gutter-width columns)))

   :offset-fn (fnk [total-width grid-width column-width gutter-width]
                       (fn [amount]
                         (+ (* total-width
                               (/ (* (+ gutter-width
                                        column-width)
                                     amount)
                                  grid-width))
                            (* total-width
                               (/ (* 0.5 gutter-width)
                                  grid-width)))))

   :create-grid (fnk [columns column-width gutter-width
                      {total-width (or total-width grid-width)}
                      grid-width offset-fn]
                       {:row (fn []
                               ["&"
                                clearfix
                                {:display "block"
                                 :width (* total-width
                                           (/ (+ gutter-width grid-width)
                                              grid-width))
                                 ;; [[ ... ]] creates lists without commas
                                 :margin [[0 (* total-width
                                                (- (/ (* 0.5 gutter-width)
                                                      grid-width)))]]}])

                        :column (fn [n]
                                  {:display "inline"
                                   :float "left"
                                   :width (* total-width
                                             (/ (- (* (+ gutter-width
                                                         column-width)
                                                      n)
                                                   gutter-width)
                                                grid-width))
                                   :margin [[0 (* total-width
                                                  (/ (* 0.5 gutter-width)
                                                     grid-width))]]})

                        :push (fn [amount]
                                {:margin-left (offset-fn amount)})

                        :pull (fn [amount]
                                {:margin-right (offset-fn amount)})})

   ;; Dynamically load in a grid from elsewhere depending on the :layout value
   ;; Not sure about this one, guess it doesn't hurt and keeps everything nicely
   ;; packed together without having to destructure elsewhere but inside the grid itself
   ;; the custom made functions column, row, push and pull which depend on your input.
   :grid-layout (fnk [{layout 'fixed} create-grid] ((resolve layout) create-grid))


   })


;;;
;;; Compilation strategy
;;;

(def grid-eager (graph/eager-compile grid-graph))
(def profiled-grid (graph/eager-compile (graph/profiled ::profile-data grid-graph)))

;;;
;;; FIXED Grid
;;;

(defn fixed
  [grid]
  (let [{:keys [row column push pull]} grid]
    (styles
     ["*" "*:after" "*:before"
      {:box-sizing "border-box"}]

     [:body
      {:width (percent 100)
       :font-family [["Georgia" :sans-serif]]
       :padding 0
       :margin 0}
      clearfix]

     (headings
      {:font-weight "normal"})

     (center
      {:width (px 960)
       :margin [0 "auto"]
       :overflow "hidden"})

     (top
      (column 12)
      {:margin-bottom (em 1)
       :color (hsl [0 0 100])
       :background (hsl [0 0 0])
       :padding (px 20)})

     (main
      center-text
      (column 9)
      {:color (hsl [0 0 40])
       :background (hsl [0 0 80])
       :padding (px 20)})

     (sidebar
      center-text
      (column 3)
      {:color (hsl [0 0 40])
       :background (hsl [0 0 80])
       :padding (px 20)}))))


;; (into {} (grid-eager {:column-width (px 60) :columns 12 :gutter-width 0
;;                       :amount 10 :total-width 900 :grid-width 1000}))
;(css [:.foo ((:column-fn (grid-eager {:gw 60})) 2)])
;; (:total-width (grid-eager {}))
;; (let [{:keys [row column push pull]} (:create-grid (grid-eager {}))]
;;   (row))

;:options (fnk [{columns col} {cw column-width} {gw gutter-width} {tw total-width} :as opts] opts)
   ;:foo (fnk [columns column-width :as t] {:t t})
   ;:foo (fnk [a b c d :as e] e)

;; A few examples how you could easily put these to use:
;; (def example-a (into {} (grid-eager {})))
;; ;example-a
;; (def example-b (->> {:grid-layout 'fluid} grid-eager :create-grid :row))
;; ;(example-b)
;; (def example-c (->> {:grid-layout 'fluid} grid-eager :create-grid :column))
;; ;(example-c 2)

