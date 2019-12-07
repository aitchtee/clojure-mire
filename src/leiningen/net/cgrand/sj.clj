(ns net.cgrand.sj
  (:require [net.cgrand.parsley :as p]))

(defrecord Node [tag children])

(defn len [n]
  (if-let [children (:children n)]
    (-> children first rseq first key)
    (count n)))

(defn- children-map [children]
  [(into (sorted-map) 
         (next (reductions (fn [[s i] n]
                             [(+ s (len n)) (inc i)])
                           [0 -1] children)))
   (vec children)])

(defn- bisect [v f x])

(def parens (p/parser {:make-node (fn [tag children]
                                    (Node. tag 
                                           (children-map children)))
                       :make-leaf nil}
              :list ["(" :list* ")"]))

(defn leaf-node [s parent doc offset]
  (reify
    javax.swing.text.Element
    (getName [this] nil)
    (getAttributes [this] nil)
    (getDocument [this] doc)
    (getParentElement [this] parent)
    (getStartOffset [this] offset)
    (getEndOffset [this]
      (+ offset (v/length node)))
    (getElementIndex [this offset] 0)
    (getElementCount [this] 0)
    (getElement [this idx] nil)
    (isLeaf [this] true)))

(defrecord Element [node doc parent offset] 
  javax.swing.text.Element
  (getName [this] (name (:tag node)))
  (getAttributes [this] nil)
  (getDocument [this] doc)
  (getParentElement [this] parent)
  (getStartOffset [this] offset)
  (getEndOffset [this]
    (+ offset (len node)))
  (getElementIndex [this offset]
    (val (first (subseq (first (:content node)) >= (- offs offset)))))
  (getElementCount [this]
    (count (second (:children node))))
  (getElement [this idx]
    (Element. (nth (second (:children node)) idx)
              doc this (+ offset XXX)))
  (isLeaf [this] 
    (instance? String node)))

(deftype Document [buf] 
  javax.swing.text.StyledDocument
  #_(addStyle [this G__294 G__295])
  #_(removeStyle [this G__296])
  (getStyle [this G__297])
  #_(setCharacterAttributes [this G__298 G__299 G__300 G__301])
  #_(setParagraphAttributes [this G__302 G__303 G__304 G__305])
  #_(setLogicalStyle [this G__306 G__307])
  (getLogicalStyle [this G__308])
  (getParagraphElement [this G__309])
  (getCharacterElement [this G__310])
  (getForeground [this G__311])
  (getBackground [this G__312])
  (getFont [this G__313])
  javax.swing.text.Document
  (getProperty [this G__314])
  (getLength [this])
  (remove [this offs len]
    (swap! buf edit offs len ""))
  (addDocumentListener [this G__317])
  (removeDocumentListener [this G__318])
  (addUndoableEditListener [this G__319])
  (removeUndoableEditListener [this G__320])
  (putProperty [this G__321 G__322])
  (insertString [this offs s attrs]
    (swap! buf edit offs 0 s))
  (getText [this G__326 G__327])
  (getText [this G__328 G__329 G__330])
  (getStartPosition [this])
  (getEndPosition [this])
  (createPosition [this G__331])
  (getRootElements [this])
  (getDefaultRootElement [this]
    (parse-tree @buf))
  (render [this G__332]))