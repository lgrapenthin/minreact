(ns minreact.core
  (:require [cljsjs.react]
            [goog.object :as obj]
            [clojure.set :as set])
  (:require-macros [minreact.core :refer [genspec defreact]])
  (:refer-clojure :exclude [set!]))

(def ^:private state-key "__minreact_state")

(def ^:private props-key "__minreact_props")

(defn minreact-state
  "Return the minreact state of pure React component state s, not-found
  if not present"
  ([s] (minreact-state s nil))
  ([s not-found]
   (or (some-> s (obj/get state-key)) not-found)))

(defn minreact-props
  "Return the minreact props of pure React component props p, not-found
  if not present"
  ([p] (minreact-props p nil))
  ([p not-found]
   (or (some-> p (obj/get props-key)) not-found)))

(defn state
  "Return the components current state"
  [c]
  (minreact-state (.-state c)))

(defn props
  "Return the components current props"
  [c]
  (minreact-props (.-props c)))

(defn transact-state!
  "Set the components state to f applied to its current state and
  args."
  [c f & args]
  (.setState c (fn [react-state _]
                 (js-obj state-key
                         (apply f (minreact-state react-state) args)))))

;; NOTE om korks: Grepping large om codebases shows that in 99% of the
;; cases om/set-state! and om/update-state! are used with a single key
;; --
(defn update-state!
  "Set the components state at k to f applied to its current state and
  args. See also: transact-state!"
  [c k f & args]
  (apply transact-state! c update k f args))

(defn set-state!
  "Set the components state to newval, at k if provided."
  ([c newval]
   (.setState c (js-obj state-key newval)))
  ([c k newval]
   (transact-state! c assoc k newval)))

(def reserved-ks [:key :ref :dangerouslySetInnerHTML])

(defn- extract-reserved [props]
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
              (js-delete props k)))
          [obj props])

        :else
        [(js-obj) props]))

(def ^:private default-methods
  "Minreact default methods"
  (genspec
   props
   :state state
   (fn getDefaultProps []
     (js-obj props-key nil))
   (fn getInitialState []
     nil)
   (fn shouldComponentUpdate [next-props next-state]
     (or (not= next-props props)
         (not= next-state state)))))

;; TODO: One should be allowed to specifiy a getter that is watched
;; explicitly as an optimization.  In the normal usecase this would be
;; a keyword.  Keywords and ks vecs have equality, fns have not if
;; they are lambdas, so users get better performance if they use
;; keywords.  In this sense it would be great if vectors implemented
;; get-in.

(defn- install-watch [c [getter iref :as selector]]
  (let [k (gensym "minreact-watch__")]
    (transact-state! c (fn [s]
                         (-> s
                             (update-in [:watches selector] k)
                             (assoc selector (getter @iref)))))
    (add-watch iref k
               (fn [_ _ o n]
                 (let [o (getter o)
                       n (getter n)]
                   (when-not (= o n)
                     (set-state! c selector n)))))))

(defn- uninstall-watch [c [_ iref :as selector]]
  (remove-watch iref (get-in (state c) [:watches selector]))
  (transact-state! c (fn [s]
                       (-> s
                           (dissoc selector)
                           (update :watches dissoc selector)))))

(defn- normalize-selector
  [selector]
  (if (vector? selector)
    selector
    [identity selector]))
 
(defreact watch-irefs
  "React component that watches changes of irefs and invokes
  render-child with their values."
  [render-child & irefs]
  :state kvs
  (fn getInitialState [] {})
  (fn componentDidMount []
    (->> irefs
         (eduction (map normalize-selector))
         (run! (partial install-watch this))))
  (fn componentWillReceiveProps [[_ & next-irefs]]
    (let [irefs (into #{} (map normalize-selector) irefs)
          next-irefs (into #{} (map normalize-selector) irefs)
          removed (set/difference irefs next-irefs)
          added (set/difference next-irefs irefs)]
      (run! (partial install-watch this) added)
      (run! (partial uninstall-watch this) removed)))
  (fn render []
    (->> irefs
         (map (comp kvs
                    normalize-selector))
         (apply render-child))))
