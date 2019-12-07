(ns net.cgrand.sjacket.paredit
  (:require [net.cgrand.sjacket :as sj]
    [clojure.zip :as z]))

(defn selection [tree offset length]
  (let [from (+ (long offset) 0.1)
        to (+ from (long length) -0.1)]
    [(first (sj/loc-at tree from))
     (first (sj/loc-at tree to))]))

(defn normalize-selection [[from-loc to-loc]]
  (let [d (- (count (z/path from-loc)) (count (z/path to-loc)))
        from-locs (drop (max 0 d) (iterate z/up from-loc))
        to-locs (drop (max 0 (- d)) (iterate z/up to-loc))]
    (some identity 
      (map (fn [from-loc to-loc]
             (when (= (z/up from-loc) (z/up to-loc)) 
               [from-loc to-loc]))
        from-locs to-locs))))

(defn offset+length [[from-loc to-loc]]
  (let [normalized-offset (sj/offset-of from-loc)
        normalized-length (- (sj/offset-of to-loc true) normalized-offset)]
    [normalized-offset normalized-length]))

(defn nodes [[from-loc to-loc :as sel]]
  (assert (= sel (normalize-selection sel)))
  (map z/node
    (take (inc (- (count (z/lefts to-loc)) (count (z/lefts from-loc))))
      (iterate z/right from-loc))))

(defn transform-selection-loc [sel f & args]
  (let [nodes (nodes sel)
        [exprs ctx] (sj/to-sexprs nodes)
        expr' (apply f exprs args)
        node (sj/to-pt expr' ctx)
        [from-loc to-loc] sel
        nloc (nth (iterate (comp z/next z/remove) from-loc) (dec (count nodes)))
        nloc (z/replace nloc node)
        delta (- (sj/column nloc true) (sj/column to-loc true))]
    (sj/subedit nloc sj/shift-right delta)))

(defn transform-selection-src  [src offset length f & args]
  (let [ptree (p/parser src)
        ptree2 (z/root (apply transform-selection-loc 
                         (normalize-selection (selection ptree offset length)) f args))]
    (sj/str-pt ptree2)))