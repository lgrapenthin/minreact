(ns dev
  (:require [mireact.core :as mr]))

(defn div [& content]
  (apply js/React.createElement "div" nil
         content))

(defn button [title on-click]
  (apply js/React.createElement "button" #js{:onClick on-click}
         title))

(mr/defreact counter [this props state]
  mixins ^:no-default []
  (fn componentWillMount []
    (mr/creset! this {:counter 400}))
  (fn raw componentWillReceiveProps [next-props]
    (js/console.log "counter got props" next-props))
  (fn render []
    (div
      (div (str "Props: " (pr-str props)))
      (div (str "State: " (pr-str state)))
      (div (str "Children: " (.. this -props -children)))
      (div
        (button "Click me" (fn [_]
                             (mr/cswap! this update :counter (fnil inc 0))))))))

(mr/defreact app [this _ {:keys [passed-props]}]
  (fn render []
    (div
      (counter passed-props)
      (button "click me too" (fn [_]
                               (mr/cswap! this assoc :passed-props 3000))))))

(defn main []
  (js/React.render (app 32)
                   (js/document.getElementById "app")))

(main)
