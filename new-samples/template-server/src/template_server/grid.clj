(ns template-server.grid
  "Demonstration using Garden to recreate the Semantic Grid framework.
  Thanks to Joel Holbrook (noprompt) for the work on writing this up.
  * <https://github.com/noprompt/garden>
  * <http://semantic.gs/>
  * <https://github.com/twigkit/semantic.gs/blob/master/stylesheets/scss/grid.scss>"

  (:refer-clojure :exclude [+ - * /])
  (:require [garden.units :refer [px em percent]]
            [garden.color :refer [hsl]]
            [garden.arithmetic :refer [+ - * /]]
            [garden.def :refer [defrule]]))

(def ^{:doc "The famous \"micro\" clearfix."}
  clearfix
  ["&"
   {:*zoom 1}
   ["&:before" "&:after"
    {:content "\"\""
     :display "table"}]
   ["&:after"
    {:clear "both"}]])

;; In Sass configuration is achieved through the use of mutable
;; variables. In Clojure we can define our default configuration as an
;; immutable *value*. Later we can refer to this value when building
;; out or grid system. Note how we can use Clojure's tagged literals
;; to make our stylesheet code look more like CSS.

;; SEE: https://github.com/twigkit/semantic.gs/blob/master/stylesheets/scss/grid.scss#L6-L8
(def grid-defaults
  {:column-width (px 80)
   :gutter-width (px 0)
   :columns 12})

;; This function has been ported directly from the SCSS code.

;; SEE: https://github.com/twigkit/semantic.gs/blob/master/stylesheets/scss/grid.scss#L11-L13
(defn grid-system-width
  [{:keys [column-width columns gutter-width]}]
  (+ (* column-width columns)
     (* gutter-width columns)))

;; Since we aren't using mutable values to configure our grid system
;; and it's behavior, we need to apply a functional approach. Instead
;; of functions which simply respond to mutable values in the
;; surrounding code, we use higher order functions to create an
;; "instance" of a grid system based upon it's input values.

;; There are four key functions which are commonly used when working
;; with grid systems: `row`, `column`, `push`, and `pull`. The
;; following higher order functions assist in creating them. Each one
;; accepts grid configuration via a map and returns an instance of one
;; of these key functions.

;; https://github.com/twigkit/semantic.gs/blob/master/stylesheets/scss/grid.scss#L46-L53
(defn row-fn
  "Creates a row function based on a grid configuration."
  [{:keys [total-width grid-width gutter-width]}]
  (fn []
    ["&"
     clearfix
     {:display "block"
      :width (* total-width
                (/ (+ gutter-width grid-width)
                   grid-width))
      :margin [0 (* total-width
                    (- (/ (* 0.5 gutter-width)
                          grid-width)))]}]))

;; https://github.com/twigkit/semantic.gs/blob/master/stylesheets/scss/grid.scss#L54-L61
(defn column-fn
  "Creates a column function based on a grid configuration."
  [{:keys [total-width grid-width column-width gutter-width]}]
  (fn [n]
    {:display "inline"
     :float "left"
     :width (* total-width
               (/ (- (* (+ gutter-width
                           column-width)
                        n)
                     gutter-width)
                  grid-width))
     :margin [0 (* total-width
                   (/ (* 0.5 gutter-width)
                      grid-width))]}))

(defn offset-margin
  "Computes the offset amount for push and pull functions."
  [{:keys [total-width grid-width column-width gutter-width]} amount]
  (+ (* total-width
        (/ (* (+ gutter-width
                 column-width)
              amount)
           grid-width))
     (* total-width
        (/ (* 0.5 gutter-width)
           grid-width))))

;; Lastly, we need a function for tying everything together.

(defn create-grid
  ([]
     ;; Make use of the default grid settings.
     (create-grid grid-defaults))
  ([grid-opts]
     (let [;; Ensure we have everything we need to generate the key
           ;; functions mentioned above.
           opts (merge grid-defaults grid-opts)
           {:keys [column-width gutter-width total-width]} opts
           ;; Calculate the grid with.
           grid-width (grid-system-width grid-opts)
           opts (assoc opts
                  :grid-width grid-width
                  :total-width (or total-width grid-width))
           ;; Partially evaluate `offset-margin` with our grid options
           ;; for our custom `push` and `pull` functions.
           offset (partial offset-margin opts)]
       ;; Return a map representing an instance of our grid system.
       {:row (row-fn opts)
        :column (column-fn opts)
        :push (fn [amount]
                {:margin-left (offset amount)})
        :pull (fn [amount]
                {:margin-right (offset amount)})})))

;; With our grid factory in place let's put together a demo. For
;; demonstration purposes we'll take the easy route and build the
;; "fixed" grid (http://semantic.gs/examples/fixed/fixed.html).

;;;
;;; Convenience methods
;;;

;; I like to create this alias because it better expresses the intent
;; in the context of gardening.
(def styles list)

;; Define a few rule functions to make the code more expressive and
;; readable.

(defrule center :div.center)
(defrule top :section#top)
(defrule main :section#main)
(defrule sidebar :section#sidebar)
(defrule headings :h1 :h2 :h3)

;; I like this approach as well (mixin).
(def center-text {:text-align "center"})

;;;
;;; FIXED Grid
;;;

;TODO all others
(def fixed
  ;; Create a standard grid and bind the key values.
  (let [{:keys [row column push pull]} (create-grid)]
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