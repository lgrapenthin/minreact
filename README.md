# minreact

`[minreact "0.1.0-SNAPSHOT]` is a ClojureScript *adapter* for Facebooks React.  It is designed with the minimalistic goal of providing just enough ClojureScript to make React interop non-tedious. 

# Features

- Clojure interop philosophy: Wherever minreact has no functionality to add you are encouraged to use interop

- `defspec` and `defreact` macros make writing React specs and factories a joy

- Memorizable API to access local mutable state

- ClojureScript identity diffing (shouldComponentUpdate) by default

- Little to no overhead in comparison to plain React

# Example

This is a simple "Hello You" with a timer in minreact and sablono

```clojure
(ns dev.core
  (:require [minreact.core :as m :refer-macros [defreact]]
            [sablono.core :refer-macros [html]])
  (:import [goog Timer]))

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

(js/React.render (hello-ui "User")
                 (js/document.getElementById "app"))
                 
```

## Contribution

Please raise issues of any kind whenever you are stuck or have a great idea.

Contributions of any kind are welcome.  Remember though that this project emphasizes minimalism.  In any case, please open an issue before writing code.

## License

Copyright © 2015 Leon Grapenthin

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
