(ns dev.core
  (:require [minreact.core :as m :refer-macros [defreact]]
            [sablono.core :refer-macros [html]]))

(defreact item-ui [{:keys [id]}]
  (fn render []
    (html
      [:div
       [:div (str "Item: " id)]])))

(defreact counter-ui [txt & children]
  :state {:keys [count]}
  :this-as this
  (fn componentWillMount []
    (m/set! this {:count 42}))
  (fn render []
    (html
      [:div
       [:div
        [:button {:on-click (fn [_]
                              (m/update! this :count (fnil inc 0)))}
         "Count more!"]]
       [:div
        (str "Count: " count)
        children]])))


(defreact app []
  :state {:keys [n-items]}
  (fn render []
    (html
      [:div
       (apply counter-ui "Hi!"
              (for [i (range n-items)]
                (item-ui {:id i
                          :ref (fn [c]
                                 (js/console.log c "mounted"))
                          :key i})))
       [:div (str "n-items: " (pr-str n-items))]
       [:button {:on-click (fn [_]
                             (m/set! app :n-items 5))}
        "click me too"]])))



(defn main []
  (js/React.render (app 32)
                   (js/document.getElementById "app")))

(main)
