(ns minreact.core)

(defn- extract-opts
  "Return [opts-map spec]"
  [spec]
  (let [opts
        (->> spec
             (partition-all 2)
             (take-while (comp keyword? first))
             (map vec))]
    [(into {} opts) (drop (* 2 (count opts)) spec)]))

(defn- wrap-fn
  [prop-binding {this-sym :this-as
                 state-binding :state
                 :or {this-sym (gensym "this")}} fn-form]
  (let [raw? (= 'raw (second fn-form))
        [fn-name fn-bindings & fn-body] (nthrest fn-form (if raw? 2 1))
        fn-params (mapv (fn [i]
                          (gensym (str fn-name "_arg_" i)))
                        (range (count fn-bindings)))]
    [(name fn-name)
     `(fn ~fn-name
        ~fn-params
        ~(let [form
               `(cljs.core/this-as ~this-sym
                  (let [~prop-binding (props ~this-sym)
                        ~@(if state-binding
                            [state-binding (list `state this-sym)])
                        ~@(if (and (not raw?)
                                   (contains? '#{componentWillReceiveProps
                                                 shouldComponentUpdate
                                                 componentWillUpdate
                                                 componentDidUpdate}
                                              fn-name))
                            (mapcat (fn [binding param getter]
                                      [binding (list getter param)])
                                    fn-bindings
                                    fn-params
                                    [`minreact-props
                                     `minreact-state])
                            (mapcat (fn [binding param]
                                      [binding param])
                                    fn-bindings
                                    fn-params))]
                    ~@fn-body))]
           (if raw?
             form
             (case fn-name
               getInitialState
               `(cljs.core/js-obj state-key ~form)
               ;; Questionable:
               
               ;; getDefaultProps
               ;; `(cljs.core/js-obj props-key ~form)

               ;; I have decided against this for now, because we
               ;; can't get Reacts behavior which does some kind of
               ;; deep merge on ClojureScript datastructures.
               form))))]))

(defmacro genspec
  "Generate a spec suitable for React.createClass.

  (genspec prop-binding options* specs*)

  prop-binding - A binding form which is available in all direct
  function definitions

  the following options are available:

  :state - A binding form for the component local state, available in
  all direct function defintions
  
  :this-as - A symbol bound to the React component in all direct
  function definitions

  spec - Either a symbol value pair, or a single literal function
  definition:

  (fn modifiers* name arg positional-params body)
  
  These function definitions are transformed so that state and props
  bindings are available locally.

  The following fn names are augmented:

  componentWillReceiveProps
  shouldComponentUpdate
  componentWillUpdate
  componentDidUpdate
  getInitialState

  For these, the props and state arguments are rebound to minreact
  props and state.  The return value of getInitialState is embedded
  into a js object for React state.

  Augmentation can be disabled with the modifier \"raw\".

  mixins - If a vector is passed it is cast to a js-array.

  See https://facebook.github.io/react/docs/component-specs.html for
  available properties."
  [prop-binding & spec]
  (let [[opts spec] (extract-opts spec)
        compiled-obj
        `(cljs.core/js-obj
          ~@(loop [[f s :as elems] spec
                   result []]
              (if f
                (cond (symbol? f)
                      (recur (nthnext elems 2)
                             (conj result
                                   (name f)
                                   (cond (= 'mixins f)
                                         `(let [s# ~s]
                                            (if (vector? s#)
                                              (apply cljs.core/array s#)))
                                         :else
                                         s)))
                      (list? f)
                      (recur (next elems)
                             (into result (wrap-fn prop-binding opts f)))
                      :else
                      (throw (IllegalArgumentException.
                              (str "Invalid spec elem: " (pr-str f)))))
                result)))]
    `(-> default-methods
         (js/goog.object.clone)
         (doto (js/goog.object.extend ~compiled-obj))))) 

(defmacro defreact
  "Define a variadic factory function according to spec. varargs
  become the components minreact props.

  See the genspec docstring for spec.

  Within methods, the components this object is bound to `this`.  It
  can be overriden using the :this-as option in spec.


  React attributes:
  
  If the first arg passed to the factory is a map or JS object the
  following keys are associated directly in the React props and will
  not be made available in the minreact props:

  :key, :ref, :dangerouslySetInnerHTML

  See also:
  https://facebook.github.io/react/docs/special-non-dom-attributes.html"
  [name prop-binding & spec]
  (assert (vector? prop-binding))
  `(def ~name
     (let [c# (js/React.createClass
               (genspec ~prop-binding :this-as ~'this ~@spec))]
       (fn
         ([]
          (js/React.createElement c# (cljs.core/js-obj)))
         ([prop# & props#]
          (let [[obj# prop#] (extract-reserved prop#)]
            (js/goog.object.add obj# props-key (cons prop# props#))
            (js/React.createElement c# obj#)))))))
