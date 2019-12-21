(ns net.cgrand.parsley.pslr1)

(defn scc "Strongly connected components of a graph -- tarjan's algorithm."
  [nodes transition]
  (let [s (fn [n i lowlink]
            (if (lowlink n)
              lowlink
              (reduce min i (map #(s % )))))]))