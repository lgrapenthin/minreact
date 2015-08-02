(ns dev.core
  (:require [minreact.core :as m :refer-macros [defreact]]))

(defn div [& content]
  (apply js/React.createElement "div" nil
         content))

(defn button [title on-click]
  (apply js/React.createElement "button" #js{:onClick on-click}
         title))

(defreact item-ui [{:keys [id]}]
  (fn render []
    (div
      (div (str "Item: " id)))))

(defreact counter-ui [txt & children]
  :state {:keys [count]}
  (fn componentWillMount []
    (m/set! counter-ui {:count 42}))
  (fn render []
    (div
      (div
        (button "Count more!"
          (fn [_]
            (m/update! counter-ui :count (fnil inc 0)))))
      (apply div
             (str "Count: " count)
             children))))

(defreact app []
  :state {:keys [n-items]}
  (fn render []
    (div
      (apply counter-ui "Hi!"
             (for [i (range n-items)]
               (item-ui {:id i
                         :ref (fn [c]
                                (js/console.log c "mounted"))
                         :key i})))
      (div (str "n-items: " (pr-str n-items)))
      (button "click me too"
        (fn [_]
          (m/set! app :n-items 5))))))

(defn main []
  (js/React.render (app 32)
                   (js/document.getElementById "app")))

(main)
