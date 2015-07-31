(ns dev.core
  (:require [mireact.core :as mr :refer-macros [defreact]]))

(defn div [& content]
  (apply js/React.createElement "div" nil
         content))

(defn button [title on-click]
  (apply js/React.createElement "button" #js{:onClick on-click}
         title))

(defreact counter-ui [txt & children]
  :state {:keys [count]}
  (fn componentWillMount []
    (mr/set! counter-ui {:count 42}))
  (fn render []
    (div
      (div
        (button "Count more!"
          (fn [_]
            (mr/update! counter-ui :count (fnil inc 0)))))
      (apply div
             (str "Count: " count)
             children))))

(defreact app []
  :state {:keys [n-divs]}
  (fn render []
    (div
      (apply counter-ui "Hi!"
             (for [i (range n-divs)]
               (div i)))
      (div (str "n-divs: " (pr-str n-divs)))
      (button "click me too"
        (fn [_]
          (mr/set! app :n-divs 5))))))

(defn main []
  (js/React.render (app 32)
                   (js/document.getElementById "app")))

(main)
