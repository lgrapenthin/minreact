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
    (m/set-state! component {:count 42}))
  (fn render []
    (html
      [:div
       [:div
        [:button {:on-click (fn [_]
                              (m/state! component update :count (fnil inc 0)))}
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
  (fn getInitialState []
    {:seconds-passed 0
     :timer (Timer. 1000)})
  (fn componentDidMount []
    (.listen timer Timer.TICK
             #(m/state! this update :seconds-passed inc))
    (.start timer))
  (fn componentWillUnmount []
    (.stop timer)))

(defonce some-atom (atom {}))

(defreact watch-test []
  :state {:keys [v?]}
  (fn render []
    (html
      [:div
       (if v?
         (m/with-irefs [s some-atom]
           (html [:div (pr-str s)])))
       [:button {:on-click (fn [e]
                             (m/state! this update :v? not))}
        (pr-str v?)]])))

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
                             (m/set-state! this :n-items 5))}
        "click me too"]
       [:div (hello-ui "Beate")]
       (watch-test)
       
       [:button {:on-click (fn [_]
                             (swap! some-atom update :v inc))}
        "Increase atom"]])))

(defn main []
  (js/React.render (app)
                   (js/document.getElementById "app")))

(main)
