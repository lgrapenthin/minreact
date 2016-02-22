# minreact

`[minreact "0.1.6]` is a ClojureScript *adapter* for Facebooks React.  It is designed with the minimalistic goal of providing just enough ClojureScript to make React interop non-tedious. 

# Features

- Follows Clojures interop philosophy: Wherever minreact has no functionality to add you are encouraged to use interop

- `defspec` and `defreact` macros make writing React specs and factories look and feel both idiomatic and transparent at the same time

- Memorizable API to access local mutable state

- `with-iref` component and macro to bind components to ClojureScript IRefs like atoms.

- opt-in `wrapping` modifier for JS compatible wrapper components

- ClojureScript identity diffing (shouldComponentUpdate) by default

- Little to no overhead in comparison to plain React

# Example

This is a simple "Hello User" with a timer in minreact and sablono

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
  (fn getInitialState []
    {:seconds-passed 0
     :timer (Timer. 1000)})
  (fn componentDidMount []
    (.listen timer Timer.TICK
             #(m/state! this update :seconds-passed inc))
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
