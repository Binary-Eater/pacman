(ns pacman.logic
  (:require [ubergraph.core :as uber])
  (:use [clojure.set]))

(defn eatHelper
  [mz_pt y_idx]
  (loop [y y_idx
         mz_x (map-indexed vector mz_pt)
         set_data []]
    (if (empty? mz_x)
      set_data
      (recur y_idx
             (rest mz_x)
             (if (or (= (second (first mz_x)) \·) (= (second (first mz_x)) \●))
               (conj set_data (hash-map :x (first (first mz_x)), :y y_idx))
               set_data)))
    )
  )

(defn getEatSet
  [mz_dt]
  (loop [idx_vec (map-indexed vector mz_dt)
         eat_set #{}]
    (if (empty? idx_vec)
      eat_set
      (recur (rest idx_vec)
             (union eat_set (set (eatHelper (second (first idx_vec)) (first (first idx_vec)))))))
    )
  )

(defn redHelper
  [mz_pt y_idx]
  (loop [y y_idx
         mz_x (map-indexed vector mz_pt)
         set_data []]
    (if (empty? mz_x)
      set_data
      (recur y_idx
             (rest mz_x)
             (if  (= (second (first mz_x)) \R)
               (conj set_data (hash-map :x (first (first mz_x)), :y y_idx))
               set_data)))
    )
  )

(defn pinkHelper
  [mz_pt y_idx]
  (loop [y y_idx
         mz_x (map-indexed vector mz_pt)
         set_data []]
    (if (empty? mz_x)
      set_data
      (recur y_idx
             (rest mz_x)
             (if  (= (second (first mz_x)) \P)
               (conj set_data (hash-map :x (first (first mz_x)), :y y_idx))
               set_data)))
    )
  )

(defn orangeHelper
  [mz_pt y_idx]
  (loop [y y_idx
         mz_x (map-indexed vector mz_pt)
         set_data []]
    (if (empty? mz_x)
      set_data
      (recur y_idx
             (rest mz_x)
             (if  (= (second (first mz_x)) \O)
               (conj set_data (hash-map :x (first (first mz_x)), :y y_idx))
               set_data)))
    )
  )

(defn blueHelper
  [mz_pt y_idx]
  (loop [y y_idx
         mz_x (map-indexed vector mz_pt)
         set_data []]
    (if (empty? mz_x)
      set_data
      (recur y_idx
             (rest mz_x)
             (if  (= (second (first mz_x)) \B)
               (conj set_data (hash-map :x (first (first mz_x)), :y y_idx))
               set_data)))
    )
  )

(defn getRedVec
  [mz_dt]
  (loop [idx_vec (map-indexed vector mz_dt)
         eat_set []]
    (if (empty? idx_vec)
      eat_set
      (recur (rest idx_vec)
             (into [] (concat eat_set (redHelper (second (first idx_vec)) (first (first idx_vec)))))))
    )
  )

(defn getPinkVec
  [mz_dt]
  (loop [idx_vec (map-indexed vector mz_dt)
         eat_set []]
    (if (empty? idx_vec)
      eat_set
      (recur (rest idx_vec)
             (into [] (concat eat_set (pinkHelper (second (first idx_vec)) (first (first idx_vec)))))))
    )
  )

(defn getOrangeVec
  [mz_dt]
  (loop [idx_vec (map-indexed vector mz_dt)
         eat_set []]
    (if (empty? idx_vec)
      eat_set
      (recur (rest idx_vec)
             (into [] (concat eat_set (orangeHelper (second (first idx_vec)) (first (first idx_vec)))))))
    )
  )

(defn getBlueVec
  [mz_dt]
  (loop [idx_vec (map-indexed vector mz_dt)
         eat_set []]
    (if (empty? idx_vec)
      eat_set
      (recur (rest idx_vec)
             (into [] (concat eat_set (blueHelper (second (first idx_vec)) (first (first idx_vec)))))))
    )
  )

(defn buildScatterMap
  [mz_dt]
  (hash-map :red (getRedVec mz_dt), :pink (getPinkVec mz_dt), :orange (getOrangeVec mz_dt), :blue (getBlueVec mz_dt))
  )

(defn startHelper
  [mz_pt y_idx search_chr]
  (loop [y y_idx
         mz_x (map-indexed vector mz_pt)
         start_map {}]
    (if (or (empty? mz_x) (-> start_map empty? not))
      start_map
      (recur y_idx
             (rest mz_x)
             (if  (= (second (first mz_x)) search_chr)
               (hash-map :x (first (first mz_x)), :y y_idx)
               start_map)))
    )
  )

(defn getStart
  [mz_dt search_chr]
  (loop [idx_vec (map-indexed vector mz_dt)
         start_map {}]
    (if (or (empty? idx_vec) (-> start_map empty? not))
      start_map
      (recur (rest idx_vec)
             (startHelper (second (first idx_vec)) (first (first idx_vec)) search_chr))
    )
  ))

(defn getPlayerStart
  [mz_dt]
  (getStart mz_dt \S))

(defn getRedStart
  [mz_dt]
  (getStart mz_dt \H))

(defn getPinkStart
  [mz_dt]
  (getStart mz_dt \J))

(defn getOrangeStart
  [mz_dt]
  (getStart mz_dt \K))

(defn getBlueStart
  [mz_dt]
  (getStart mz_dt \L))

(defn isMoveableChar
  [chr]
  (cond (= chr \space) true
        (= chr \·) true
        (= chr \●) true
        :else false)
  )

(defn isSlowChar
  [chr]
  (if (= chr \#)
    true
    false))

(defn isTeleport
  [chr]
  (if (= chr \<)
   true
   false))

(defn isUp
  [chr]
  (if (= chr \^)
    true
    false)
  )

(defn graphUpHelper
  [mz_2d x y]
  (if (or (isMoveableChar (aget mz_2d y x)) (isSlowChar (aget mz_2d y x)) (isTeleport (aget mz_2d y x)))
  (if (> y 0)
    (cond
      (isMoveableChar (aget mz_2d (- y 1) x)) (vector (hash-map :x x, :y y) (hash-map :x x, :y (- y 1)) 1)
      (isSlowChar (aget mz_2d (- y 1) x)) (vector (hash-map :x x, :y y) (hash-map :x x, :y (- y 1)) 5)
      :else [])
    []
    )
  [])
  )

(defn graphDownHelper
  [mz_2d x y]
  (if (or (isMoveableChar (aget mz_2d y x)) (isSlowChar (aget mz_2d y x)) (isTeleport (aget mz_2d y x)) (isUp (aget mz_2d y x)))
    (if (< y (- (alength mz_2d) 1))
      (cond
        (isMoveableChar (aget mz_2d (+ y 1) x)) (vector (hash-map :x x, :y y) (hash-map :x x, :y (+ y 1)) 1)
        (isSlowChar (aget mz_2d (+ y 1) x)) (vector (hash-map :x x, :y y) (hash-map :x x, :y (+ y 1)) 5)
        :else [])
      []
      )
    [])
  )

(defn graphLeftHelper
  [mz_2d x y]
  (if (or (isMoveableChar (aget mz_2d y x)) (isSlowChar (aget mz_2d y x)) (isTeleport (aget mz_2d y x)) (isUp (aget mz_2d y x)))
    (if (> x 0)
      (cond
        (isMoveableChar (aget mz_2d y (- x 1))) (vector (hash-map :x x, :y y) (hash-map :x (- x 1), :y y) 1)
        (isSlowChar (aget mz_2d y (- x 1))) (vector (hash-map :x x, :y y) (hash-map :x (- x 1), :y y) 5)
        :else [])
      []
      )
    [])
  )

(defn graphRightHelper
  [mz_2d x y]
  (if (or (isMoveableChar (aget mz_2d y x)) (isSlowChar (aget mz_2d y x)) (isTeleport (aget mz_2d y x)) (isUp (aget mz_2d y x)))
    (if (< x (- (alength (aget mz_2d y)) 1))
      (cond
        (isMoveableChar (aget mz_2d y (+ x 1))) (vector (hash-map :x x, :y y) (hash-map :x (+ x 1), :y y) 1)
        (isSlowChar (aget mz_2d y (+ x 1))) (vector (hash-map :x x, :y y) (hash-map :x (+ x 1), :y y) 5)
        :else [])
      []
      )
    [])
  )

(defn graphMoveHelper
  [mz_2d x y]
  (if (= (aget mz_2d y x) \<)
    (filterv (fn [k] (not (empty? k))) (vector (graphUpHelper mz_2d x y) (graphDownHelper mz_2d x y) (graphLeftHelper mz_2d x y) (graphRightHelper mz_2d x y) (vector (hash-map :x x, :y y) (hash-map :x (- (alength (aget mz_2d y)) 1), :y y) 1)))
    (filterv (fn [k] (not (empty? k))) (vector (graphUpHelper mz_2d x y) (graphDownHelper mz_2d x y) (graphLeftHelper mz_2d x y) (graphRightHelper mz_2d x y))))
  )

(defn buildTraversalGraph
  [mz_dt]
  (loop [mz_2d (to-array-2d mz_dt)
         graph (uber/graph)
         x 0
         y 0]
    (if (>= y (alength mz_2d))
      graph
      (recur mz_2d
             (uber/add-edges* graph (graphMoveHelper mz_2d x y))
             (if (< x (- (alength (aget mz_2d y)) 1))
               (+ x 1)
               0)
             (if (< x (- (alength (aget mz_2d y)) 1))
               y
               (+ y 1))
             ))
    )
  )
