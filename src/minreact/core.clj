(ns minreact.core)

(defn- extract-opts
  "Return [opts-map spec]"
  [spec]
  (let [opts
        (into {}
              (comp
               (partition-all 2)
               (take-while (comp keyword? first)))
              spec)]
    [opts (drop (* 2 (count opts)) spec)]))

(defn- wrap-fn
  [prop-binding {this-sym :this-as
                 state-binding :state
                 :or {this-sym (gensym "this")}} fn-form]
  (let [raw? (= 'raw (second fn-form))
        [fn-name fn-bindings & fn-body] (nthrest fn-form (if raw? 2 1))
        fn-params (mapv (fn [param]
                          (gensym (name param)))
                        fn-bindings)]
    [(name fn-name)
     `(fn ~fn-name
        ~fn-params
        (cljs.core/this-as ~this-sym
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
            ~@fn-body)))]))

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

  A spec consists of either of a symbol value pair:

  Defines a field in the generated spec named symbol and definition.

  See https://facebook.github.io/react/docs/component-specs.html for
  available properties.

  The following properties are augmented:

  mixins - If a vector is passed, the minreact default mixin is
  prepended and it is cast to a js-array.  The default mixin can be
  disabled by associating :no-default on the vectors metadata map.
  NOTE: If no mixins are passed, the minreact default mixin is used as
  well.

  Alternatively, a single literal function definition can be passed as
  follows

  (fn modifiers* name arg positional-params body)
  
  These function definitions are transformed so that state and props
  bindings are available.

  The following fn names are augmented:

  componentWillReceiveProps
  shouldComponentUpdate
  componentWillUpdate
  componentDidUpdate

  For these, the props and state arguments passed by react are rebound
  to their minreact properties.  You can disable this behavior by
  specifying the symbol \"raw\" as a modifier.

  Note that this behavior can be avoided completely by specifiyng the
  function as a symbol definition pair."
  [prop-binding & spec]
  (let [[opts spec] (extract-opts spec)
        spec (cond-> spec
               (not-any? #{'mixins} spec)
               (concat '[mixins []]))]
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
                                          (apply cljs.core/array
                                                 (cond->> s#
                                                   (not (:no-default (meta s#)))
                                                   (cons default-mixin)))))
                                     :else
                                     s)))
                  (list? f)
                  (recur (next elems)
                         (into result (wrap-fn prop-binding opts f)))
                  :else
                  (throw (IllegalArgumentException.
                          (str "Invalid spec elem: " (pr-str f)))))
            result))))) 

(defmacro defreact
  "Define a variadic factory function according to spec. varargs
  become the components props.

  The component is available under the same name as the component.
  This can be overriden via the :this-as option in spec.

  See also: genspec."
  [name prop-binding & spec]
  (assert (vector? prop-binding))
  `(def ~name
     (let [c# (js/React.createClass
               (genspec ~prop-binding :this-as ~name ~@spec))]
       (fn [& props#]
         (js/React.createElement c# (cljs.core/js-obj props-key props#))))))
