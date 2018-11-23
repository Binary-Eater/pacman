(ns pacman.ai
  (:require [ubergraph.core :as uber]
            [ubergraph.alg :as uberalg]
            [clojure.core.match :refer [match]])
  (:use [pacman.logic :as logic]))

(defrecord ghost [pos target mode type pred scatter_idx life fright_time])

(defn frightenedMove
  [move_graph pos pred]
  (let [traverse_graph (uber/out-edges move_graph pos)]
    (if (-> traverse_graph empty? not)
      (let [move (uber/dest (rand-nth traverse_graph))]
        (if (= (count traverse_graph) 1)
          move
          (if (= move pred)
            (frightenedMove move_graph pos pred)
            move))
        )
      pos)
    )
  )

(defn updateScatterIdx
  [pos scatter_map type scatter_idx]
  (if (= pos ((get scatter_map type) scatter_idx))
    (if (< (+ scatter_idx 1) (count (get scatter_map type)))
      (+ scatter_idx 1)
      0)
    scatter_idx)
  )

(defn scatterMove
  [move_graph pos scatter_map type scatter_idx pred]
  (if (= pos ((get scatter_map type) scatter_idx))
    (frightenedMove move_graph pos pred)
    (uber/dest (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos ((get scatter_map type) scatter_idx))))))
  )

(defn redMove
  [move_graph pos target]
  (if (= pos target)
    pos
    (uber/dest (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos target))))
    ))

(defn pinkMove
  [move_graph pos target dir]
  (case dir
    :up (if (nil? (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (update (update target :y - 4) :x - 4)))))
          (redMove move_graph pos target)
          (uber/dest (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (update (update target :y - 4) :x - 4))))))
    :down (if (nil? (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (update target :y + 4)))))
            (redMove move_graph pos target)
            (uber/dest (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (update target :y + 4))))))
    :left (if (nil? (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (update target :x - 4)))))
            (redMove move_graph pos target)
            (uber/dest (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (update target :x - 4))))))
    (if (nil? (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (update target :x + 4)))))
      (redMove move_graph pos target)
      (uber/dest (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (update target :x + 4))))))
    ))

(defn blueMove
  [move_graph pos pred target dir red_ai_pos]
  (case dir
    :up (if (nil? (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (assoc (assoc target :y (+ (- (get target :y) 2) (- (- (get target :y) 2) (get red_ai_pos :y)))) :x (+ (get target :x) (- (get target :x) (get red_ai_pos :x))))))))
          (frightenedMove move_graph pos pred)
          (uber/dest (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (assoc (assoc target :y (+ (- (get target :y) 2) (- (- (get target :y) 2) (get red_ai_pos :y)))) :x (+ (get target :x) (- (get target :x) (get red_ai_pos :x)))))))))
    :down (if (nil? (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (assoc (assoc target :y (+ (+ (get target :y) 2) (- (+ (get target :y) 2) (get red_ai_pos :y)))) :x (+ (get target :x) (- (get target :x) (get red_ai_pos :x))))))))
            (frightenedMove move_graph pos pred)
            (uber/dest (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (assoc (assoc target :y (+ (+ (get target :y) 2) (- (+ (get target :y) 2) (get red_ai_pos :y)))) :x (+ (get target :x) (- (get target :x) (get red_ai_pos :x)))))))))
    :left (if (nil? (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (assoc (assoc target :x (+ (- (get target :x) 2) (- (- (get target :x) 2) (get red_ai_pos :x)))) :y (+ (get target :y) (- (get target :y) (get red_ai_pos :y))))))))
            (frightenedMove move_graph pos pred)
            (uber/dest (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (assoc (assoc target :x (+ (- (get target :x) 2) (- (- (get target :x) 2) (get red_ai_pos :x)))) :y (+ (get target :y) (- (get target :y) (get red_ai_pos :y)))))))))
    (if (nil? (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (assoc (assoc target :x (+ (+ (get target :x) 2) (- (+ (get target :x) 2) (get red_ai_pos :x)))) :y (+ (get target :y) (- (get target :y) (get red_ai_pos :y))))))))
      (frightenedMove move_graph pos pred)
      (uber/dest (first (uberalg/edges-in-path (uberalg/shortest-path move_graph pos (assoc (assoc target :x (+ (+ (get target :x) 2) (- (+ (get target :x) 2) (get red_ai_pos :x)))) :y (+ (get target :y) (- (get target :y) (get red_ai_pos :y)))))))))
    )
  )

(defn hypot [x y]
  (let [x2 (* x x)
        y2 (* y y)]
    (Math/sqrt (+ x2 y2)))
  )

(defn orangeMove
  [move_graph pos target scatter_map scatter_idx type pred]
  (if (< (hypot (- (get pos :x) (get target :x)) (- (get pos :y) (get target :y))) 8)
    (scatterMove move_graph pos scatter_map type scatter_idx pred)
    (redMove move_graph pos target))
  )

(defn chaseMove
  [move_graph pos pred target type dir red_ai_pos scatter_map scatter_idx]
  (case type
    :red (redMove move_graph pos target)
    :pink (pinkMove move_graph pos target dir)
    :blue (blueMove move_graph pos pred target dir red_ai_pos)
    :orange (orangeMove move_graph pos target scatter_map scatter_idx type pred)
    )
  )

(defn initAi
  [start_coord target_coord mode type]
  (ghost. start_coord target_coord mode type start_coord 0 0 0))

(defn get-ai-dir
  [ai]
  (cond
    (= (- (get (get ai :pos) :x) (get (get ai :pred) :x)) -1) :left
    (= (- (get (get ai :pos) :x) (get (get ai :pred) :x)) 1) :right
    (= (- (get (get ai :pos) :y) (get (get ai :pred) :y)) -1) :up
    (= (- (get (get ai :pos) :y) (get (get ai :pred) :y)) 1) :down
    :else :stop)
  )

(defn mode-handler
  [ai time_interval]
  (if (= (:mode ai) :stop)
    ai
    (cond (= (:life ai) 0) (assoc ai :mode :stop)
          (<= (:life ai) (/ 25 time_interval)) (assoc ai :mode :scatter)
          (and (> (:life ai) (/ 25 time_interval)) (<= (:life ai) (/ 55 time_interval))) (assoc ai :mode :chase)
          (and (> (:life ai) (/ 55 time_interval)) (<= (:life ai) (/ 80 time_interval))) (assoc ai :mode :scatter)
          (and (> (:life ai) (/ 80 time_interval)) (<= (:life ai) (/ 110 time_interval))) (assoc ai :mode :chase)
          (and (> (:life ai) (/ 110 time_interval)) (<= (:life ai) (/ 130 time_interval))) (assoc ai :mode :scatter)
          (and (> (:life ai) (/ 130 time_interval)) (<= (:life ai) (/ 160 time_interval))) (assoc ai :mode :chase)
          (and (> (:life ai) (/ 160 time_interval)) (<= (/ 180 time_interval) (:life ai))) (assoc ai :mode :scatter)
          :else (assoc ai :mode :chase)))
  )

(defn schedule-ai
  [ai time_interval]
  (if (= (:mode ai) :frightened)
    (if (>= (:fright_time ai) (/ 15 time_interval))
      (assoc (mode-handler ai time_interval) :fright_time 0)
      ai)
    (mode-handler ai time_interval))
  )

(defn update-ai
  [ai move_graph scatter_map player_dir red_ai_pos]
  (match (vector (:mode ai))
         [:chase] (update (assoc (assoc (assoc ai :pos (chaseMove move_graph (get ai :pos) (get ai :pred) (get ai :target) (get ai :type) player_dir red_ai_pos scatter_map (get ai :scatter_idx))) :pred (get ai :pos)) :scatter_idx (updateScatterIdx (get ai :pos) scatter_map (get ai :type) (get ai :scatter_idx))) :life + 1)
         [:frightened] (update (assoc (assoc ai :pos (frightenedMove move_graph (get ai :pos) (get ai :pred))) :pred (get ai :pos)) :fright_time + 1)
         [:scatter]  (update (assoc (assoc (assoc ai :pos (scatterMove move_graph (get ai :pos) scatter_map (get ai :type) (get ai :scatter_idx) (get ai :pred))) :pred (get ai :pos)) :scatter_idx (updateScatterIdx (get ai :pos) scatter_map (get ai :type) (get ai :scatter_idx))) :life + 1)
         [:stop] ai
         [_] ai
         )
  )
