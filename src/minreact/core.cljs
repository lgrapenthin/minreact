(ns minreact.core
  (:require [cljsjs.react])
  (:require-macros [minreact.core :refer [genspec]])
  (:refer-clojure :exclude [set!]))

(def ^:private state-key "__minreact_state")

(def ^:private props-key "__minreact_props")

(defn minreact-state
  "Return the minreact state of pure React component state s, not-found
  if not present"
  ([s] (minreact-state s nil))
  ([s not-found]
   (or (some-> s (aget state-key)) not-found)))

(defn minreact-props
  "Return the minreact props of pure React component props p, not-found
  if not present"
  ([p] (minreact-props p nil))
  ([p not-found]
   (or (some-> p (aget props-key)) not-found)))

(defn state
  "Return the components current state"
  [c]
  (minreact-state (.-state c)))

(defn props
  "Return the components current props"
  [c]
  (minreact-props (.-props c)))

(defn transact!
  "Set the components state to f applied to its current state and
  args."
  [c f & args]
  (.setState c (fn [react-state _]
                 (js-obj state-key
                         (apply f (minreact-state react-state) args)))))

;; NOTE om korks: Grepping large om codebases shows that in 99% of the
;; cases om/set-state! and om/update-state! are used with a single key
;; --
(defn update!
  "Set the components state at k to f applied to its current state and
  args. See also: transact!"
  [c k f & args]
  (apply transact! c update k f args))

(defn set!
  "Set the components state to newval, at k if provided."
  ([c newval]
   (.setState c (js-obj state-key newval)))
  ([c k newval]
   (transact! c assoc k newval)))

(def default-mixin
  "Minreact default mixin."
  (genspec
   props
   :state state
   mixins ^:no-default []
   (fn getDefaultProps []
     (js-obj props-key nil))
   (fn getInitialState []
     (js-obj state-key nil))
   (fn shouldComponentUpdate [next-props next-state]
     (or (not= next-props props)
         (not= next-state state)))))
 