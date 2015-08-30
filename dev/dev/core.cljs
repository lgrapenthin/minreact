(ns dev.core
  (:require [minreact.core :as m :refer-macros [defreact with-irefs]]
            [sablono.core :refer-macros [html]])
  (:import [goog Timer]))

(defreact item-ui [{:keys [id]}]
  (fn render []
    (html
      [:div
       [:div (str "Item: " id)]])))

(defreact counter-ui [txt & children]
  :state {:keys [count]}
  :this-as component
  (fn componentWillMount []
    (m/set! component {:count 42}))
  (fn render []
    (html
      [:div
       [:div
        [:button {:on-click (fn [_]
                              (m/update! component :count (fnil inc 0)))}
         "Count more!"]]
       [:div
        (str "Count: " count)
        children]])))

(defreact hello-ui [your-name]
  :state {:keys [seconds-passed timer]}
  (fn render []
    (html
      [:div (str "Hello, " your-name "!"
                 " I know you since " seconds-passed " seconds.")]))
  (fn componentWillMount []
    (m/set! this
            {:seconds-passed 0
             :timer
             (doto (Timer. 1000)
               (.listen Timer.TICK
                        #(m/update! this :seconds-passed inc)))}))
  (fn componentDidMount []
    (.start timer))
  (fn componentWillUnmount []
    (.stop timer)))

(def an-atom (atom 42))

(defreact app []
  :state {:keys [n-items]}
  (fn getInitialState []
    {:n-items  0})
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
                             (m/set! this :n-items 5))}
        "click me too"]
       [:div (hello-ui "Beate")]
       (with-irefs [v an-atom]
         (html [:div "Atom: " v]))
       [:button {:on-click (fn [_]
                             (swap! an-atom inc))}
        "Increase atom"]])))

(defn main []
  (js/React.render (app)
                   (js/document.getElementById "app")))

(main)
