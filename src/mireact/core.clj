(ns mireact.core)

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
                            [`mireact-props
                             `mireact-state])
                    (mapcat (fn [binding param]
                              [binding param])
                            fn-bindings
                            fn-params))]
            ~@fn-body)))]))

(defmacro genspec
  [prop-binding & spec]
  (let [[opts spec] (extract-opts spec)]
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
  [name prop-binding & spec]
  (assert (vector? prop-binding))
  (if (some #{'mixins} spec)
    `(def ~name
       (let [c# (js/React.createClass
                 (genspec ~prop-binding :this-as ~name ~@spec))]
         (fn [& props#]
           (js/React.createElement c# (cljs.core/js-obj props-key props#)))))
    `(defreact ~name
       ~prop-binding
       ~@(concat spec ['mixins []]))))
