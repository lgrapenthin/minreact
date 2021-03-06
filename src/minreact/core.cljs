(ns minreact.core
  (:require [cljsjs.react]
            [goog.object :as obj]
            [clojure.set :as set])
  (:require-macros [minreact.core :refer [genspec defreact]])
  (:refer-clojure :exclude [set!]))

(def state-key
  "Key in component local state under which minreact stores its state
  as a Clojure data structure"
  "__minreact_state")

(def props-key
  "Key in component props under which minreact stores its props as a
  Clojure data structure"
  "__minreact_props")

(def ^:dynamic *current-component*
  "If in a render method, the currently rendered component"
  nil)

(defn minreact-state
  "Return the minreact state of pure React component state s, not-found
  if not present"
  ([s] (minreact-state s nil))
  ([s not-found]
   (some-> s (obj/get state-key not-found))))

(defn minreact-props
  "Return the minreact props of pure React component props p, not-found
  if not present"
  ([p] (minreact-props p nil))
  ([p not-found]
   (some-> p (obj/get props-key not-found))))

(defn state
  "Return the components current state"
  [c]
  (minreact-state (.-state c)))

(defn props
  "Return the components current props"
  [c]
  (minreact-props (.-props c)))

(defn state!
  "Set the components state to f applied to its current state and
  args."
  [c f & args]
  (.setState c (fn [react-state _]
                 (js-obj state-key
                         (apply f (minreact-state react-state) args)))))

(defn state!!
  "Set the components state to f applied to its current state, render
  it and then invoke on-rendered."
  [c f on-rendered]
  (.setState c (fn [react-state _]
                 (js-obj state-key
                         (f (minreact-state react-state))))
             on-rendered))

;; NOTE om korks: Grepping large om codebases shows that in 99% of the
;; cases om/set-state! and om/update! are used with a single key

(defn set-state!
  "Set the components state to newval, at k if provided."
  ([c newval]
   (.setState c (js-obj state-key newval)))
  ([c k newval]
   (state! c assoc k newval)))

(def reserved-ks [:key :ref :dangerouslySetInnerHTML])

(defn extract-reserved [props]
  (cond (map? props)
        [(let [obj (js-obj)]
           (doseq [k reserved-ks]
             (when-let [v (get props k)]
               (aset obj (name k) v)))
           obj)
         (apply dissoc props reserved-ks)]

        (object? props)
        (let [obj (js-obj)]
          (doseq [k (map name reserved-ks)]
            (when-let [v (obj/get props k)]
              (aset obj k v)
              (obj/remove props k)))
          [obj props])

        :else
        [(js-obj) props]))

(def default-methods
  "Minreact default methods"
  (genspec
   props
   :raw true
   :state state
   (fn getDefaultProps []
     (js-obj props-key nil))
   (fn getInitialState []
     nil)
   (fn shouldComponentUpdate [next-props next-state]
     (or (not= next-props props)
         (not= next-state state)))))

(defn- install-watch [c [getter iref :as selector]]
  (let [k (gensym "minreact-watch__")]
    (state! c assoc
            :watch-key k
            :value (getter @iref))
    (add-watch iref k
               (fn [_ _ o n]
                 (let [o (getter o)
                       n (getter n)]
                   (when-not (= o n)
                     (set-state! c :value n)))))))

(defn- uninstall-watch [c [_ iref :as selector]]
  (remove-watch iref (:watch-key (state c)))
  (state! c #(dissoc % :watch-key :value)))

(defn- normalize-selector
  [selector]
  (if (vector? selector)
    selector
    [identity selector]))

(def irefs
  "EXPERIMENTAL.  Mixin that is required in a component that uses
  minreact.core/bind"
  (letfn [(remove-all! [this]
            (doseq [[r _] @(obj/get this "__minreact_bind")]
              (remove-watch r this)))
          (install-all! [this]
            (doseq [[r sels] @(obj/get this "__minreact_bind")]
              (add-watch r this
                         (fn [_ _ o n]
                           (loop [[sel & sels] (seq sels)]
                             (if (and sel
                                      (= (sel o) (sel n)))
                               (recur (next sels))
                               (.forceUpdate this)))))))]
    (genspec
     _
     :this-as this
     :raw true
     (fn componentWillMount []
       (obj/set this "__minreact_bind" (atom {})))
     (fn componentWillUpdate [_ _]
       (remove-all! this)
       (reset! (obj/get this "__minreact_bind") {}))
     (fn componentDidMount []
       (install-all! this))
     (fn componentDidUpdate []
       (install-all! this))
     (fn componentWillUnmount []
       (remove-all! this)))))

(defn bind
  "EXPERIMENTAL.  The component will re-render when the irefs value
  applied to f (default: identity) and args has changed.  Returns
  current value of iref.  Requires minreact.core/irefs mixin."
  ([iref]
   (bind iref identity))
  ([iref f & args]
   (if-let [c *current-component*]
     (if-let [irefs (obj/get c "__minreact_bind")]
       (let [f (if args
                 #(apply f % args)
                 f)]
         (swap! irefs update iref (fnil conj #{}) f)
         (f @iref))
       (throw "Component must use minreact.core/irefs mixin"))
     (throw "Can't use bind outside of rendering cycle"))))

(defreact watch-iref
  "React component that watches changes of irefs and invokes
  render-child with their values.

  A selector may be:

  an IRef

  a vector [getter iref] where getter limits the observed part of
  iref.

  Note that getter should be an equal value during each invocation of
  render.

  Also refer to the with-irefs macro."
  [render-child selector]
  :state {:keys [value]}
  (fn getInitialState [] {})
  (fn componentWillMount []
    (->> (normalize-selector selector)
         (install-watch this)))
  (fn componentWillReceiveProps [[_ next-selector]]
    (let [selector (normalize-selector selector)
          next-selector (normalize-selector next-selector)]
      (when-not (= selector next-selector)
        (uninstall-watch this selector)
        (install-watch this next-selector))))
  (fn componentWillUnmount []
    (uninstall-watch this (normalize-selector selector)))
  (fn wrapping render []
    (render-child value)))
