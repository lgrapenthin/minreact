(ns mireact.core)

(comment
  (genspec
   [this props state] ;; <-- can desctructor here, can close over in fns
   displayName "The amazing counter" ;; spec 
   mixins [i-can-pass-vector] ;; will cast to array

   ;; if you use fn form you must give react name
   (fn componentWillMount []
     (mr/creset! this {:counter 0}))

   ;; fn could be ommited but that doesnt play well with
   ;; default indentaion rules

   (fn render []
     (html [:button {:on-click (fn [_]
                                 (mr/cswap! this assoc :counter inc))}
            "Click me"]))
   
   ;; no special support for static yet, I don't see utility for
   ;; static in Clojure - since we don't have to close over anything
   ;; one can just provide a plain react object

   ;; works like genspec but defs a factory under this name
   (defreact counter [this props state]
     (fn render []
       )
     )
   
   ))

(defn- wrap-fn
  [bindings fn-form]
  (let [[this-sym & bindings] bindings
        raw? (= 'raw (second fn-form))
        [fn-name fn-bindings & fn-body] (nthrest fn-form (if raw? 2 1))
        fn-params (mapv (fn [param]
                          (gensym (name param)))
                        fn-bindings)]
    (assert (vector? fn-bindings))
    [(name fn-name)
     `(fn ~fn-name
        ~fn-params
        (cljs.core/this-as ~this-sym
          (let [~@(mapcat (fn [binding getter]
                            [binding (list getter this-sym)])
                          bindings
                          [`props
                           `state])
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
  [bindings & spec]
  `(cljs.core/js-obj
    ~@(loop [[f s :as elems] spec
             result []]
        (if f
          (if (symbol? f)
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
            (recur (next elems)
                   (into result (wrap-fn bindings f))))
          result)))) 

(defmacro defreact
  [name bindings & spec]
  (if (some #{'mixins} spec)
    `(def ~name
       (let [c# (js/React.createClass
                 (genspec ~bindings ~@spec))]
         (fn [props# & children#]
           (apply js/React.createElement
                  c#
                  (cljs.core/js-obj props-key props#)
                  children#))))
    `(defreact ~name
       ~bindings
       ~'mixins []
       ~@spec)))
