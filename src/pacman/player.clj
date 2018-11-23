(ns pacman.player)

(defrecord player [pos dir score])

(defn isMoveableChar
  [chr]
  (cond (= chr \space) true
        (= chr \·) true
        (= chr \●) true
        (= chr \#) true
        (= chr \^) true
        (= chr \*) true
        (= chr \<) true
        :else false)
  )

(defn isTeleport
  [chr]
  (if (= chr \<)
    true
    false)
  )

(defn getTeleportPosition
  [mz_dt x y dir]
  (case dir
    :left (if (= x 0)
            (hash-map :x (-> y mz_dt count (- 1)), :y y)
            nil)
    :right (if (= x (-> y mz_dt count (- 1)))
             (hash-map :x 0, :y y)
             nil)
    :up (if (= y 0)
          (hash-map :x x, :y (- (count mz_dt) 1))
          nil)
    :down (if (= y (- (count mz_dt) 1))
            (hash-map :x x, :y 0)
            nil)
    nil)
  )

(defn notValidDirection
  [dir]
  (case dir
    :up false
    :down false
    :left false
    :right false
    true)
  )

(defn update-player-pos
  [mz_dt player dir]
  (let [move (if (notValidDirection dir) (get player :dir) dir)
        x (get (get player :pos) :x)
        y (get (get player :pos) :y)]
    (case move
      :up (cond (and (isTeleport ((mz_dt y) x)) (not (nil? (getTeleportPosition mz_dt x y move)))) (assoc (assoc player :pos (getTeleportPosition mz_dt x y move)) :dir move)
                (and (>= (- y 1) 0) (isMoveableChar ((mz_dt (- y 1)) x))) (assoc (assoc player :pos (update-in (get player :pos) [:y] - 1)) :dir move)
                :else (assoc player :dir move))
      :down (cond (and (isTeleport ((mz_dt y) x)) (not (nil? (getTeleportPosition mz_dt x y move)))) (assoc (assoc player :pos (getTeleportPosition mz_dt x y move)) :dir move)
                  (and (< (+ y 1) (count mz_dt)) (isMoveableChar ((mz_dt (+ y 1)) x))) (assoc (assoc player :pos (update-in (get player :pos) [:y] + 1)) :dir move)
                  :else (assoc player :dir move))
      :left (cond (and (isTeleport ((mz_dt y) x)) (not (nil? (getTeleportPosition mz_dt x y move)))) (assoc (assoc player :pos (getTeleportPosition mz_dt x y move)) :dir move)
                  (and (>= (- x 1) 0) (isMoveableChar ((mz_dt y) (- x 1)))) (assoc (assoc player :pos (update-in (get player :pos) [:x] - 1)) :dir move)
                  :else (assoc player :dir move))
      :right (cond (and (isTeleport ((mz_dt y) x)) (not (nil? (getTeleportPosition mz_dt x y move)))) (assoc (assoc player :pos (getTeleportPosition mz_dt x y move)) :dir move)
                   (and (< (+ x 1) (count (mz_dt y))) (isMoveableChar ((mz_dt y) (+ x 1)))) (assoc (assoc player :pos (update-in (get player :pos) [:x] + 1)) :dir move)
                   :else (assoc player :dir move))
      player)
    )
  )

(defn update-player-score
  [mz_dt player]
  (let [x (get (get player :pos) :x)
        y (get (get player :pos) :y)]
    (if (or (= ((mz_dt y) x) \·) (= ((mz_dt y) x) \●))
      (update player :score + 10)
      player))
  )

(defn update-board-player
  [mz_dt player]
  (let [x (get (get player :pos) :x)
        y (get (get player :pos) :y)]
    (if (or (= ((mz_dt y) x) \·) (= ((mz_dt y) x) \●))
      (assoc mz_dt y (assoc (mz_dt y) x \space))
      mz_dt))
  )
