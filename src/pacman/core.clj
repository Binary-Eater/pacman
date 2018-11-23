(ns pacman.core
  (:require [lanterna.screen :as s]
            [pacman.maze :as maze]
            [pacman.player :as player]
            [pacman.ai :as ai]
            [pacman.logic :as logic])
  (:import [pacman.player player]
           [pacman.ai ghost])
  (:gen-class))

(defn isOpposite
  [p_dir ai_dir]
  (case p_dir
    :up (if (= ai_dir :down)
          true
          false)
    :left (if (= ai_dir :right)
            true
            false)
    :right (if (= ai_dir :left)
             true
             false)
    :down (if (= ai_dir :up)
            true
            false)
    )
  )

(defn getLastKey
  [screen]
  (loop [prev-key (s/get-key screen)
         next-key (s/get-key screen)]
    (if (nil? next-key)
      prev-key
      (recur next-key
             (s/get-key screen)))
    )
  )

(defn drawAI
  [ai screen]
  (if (= (:mode ai) :frightened)
    (s/put-string screen (get (get ai :pos) :x) (get (get ai :pos) :y) "☹" {:fg :blue})
    (case (:type ai)
      :red (s/put-string screen (get (get ai :pos) :x) (get (get ai :pos) :y) "☹" {:fg :red})
      :pink (s/put-string screen (get (get ai :pos) :x) (get (get ai :pos) :y) "☹" {:fg :magenta})
      :orange (s/put-string screen (get (get ai :pos) :x) (get (get ai :pos) :y) "☹" {:fg :green})
      :blue (s/put-string screen (get (get ai :pos) :x) (get (get ai :pos) :y) "☹" {:fg :cyan})
      (s/put-string screen (get (get ai :pos) :x) (get (get ai :pos) :y) "☹" {:fg :white})))
  )

(defn updateGhostFromStop
  [ai cnd]
  (if (and cnd (= (:mode ai) :stop))
    (case (:type ai)
      :orange (assoc (update-in ai [:pos :y] - 3) :mode :scatter)
      :blue (assoc (update-in ai [:pos :y] - 3) :mode :scatter)
      (assoc (update-in ai [:pos :y] - 2) :mode :scatter))
    ai)
  )

(defn frightenedReset
  [ai start_pos target_pos]
  (case (:type ai)
    :red (ai/initAi (update start_pos :y - 2) target_pos :scatter :red)
    :pink (ai/initAi start_pos target_pos :stop :pink)
    :orange (ai/initAi start_pos target_pos :stop :orange)
    :blue (ai/initAi start_pos target_pos :stop :blue)
    (ai/initAi start_pos target_pos :stop :pink))
  )

(defn start
  [scr]
  (s/clear scr)
  (s/redraw scr)
  (s/clear scr)
  (s/put-string scr 0 0 "Welcome to the P" {:fg :blue})
  (s/put-string scr 16 0 "a" {:fg :red})
  (s/put-string scr 17 0 "c" {:fg :yellow})
  (s/put-string scr 18 0 "man Adventure Game!!!!!" {:fg :blue})
  (s/put-string scr 0 2 "Enter 0-9 for Levels 0-9 and 0xa-0xc for Levels 10-12" {:fg :white})
  (s/put-string scr 0 3 "Use arrow keys to control Pacman..... Best of luck!")
  (s/put-string scr 0 4 "Once the game has started, you can press q to quit!")
  (s/put-string scr 0 5 "Text mode can be accessed by passing -t or --text to the program.")
  (s/move-cursor scr 40 0)
  (s/redraw scr)
  (loop [lvl (getLastKey scr)]
    (case lvl
      \0 0
      \1 1
      \2 2
      \3 3
      \4 4
      \5 5
      \6 6
      \7 7
      \8 8
      \9 9
      \a 10
      \b 11
      \c 12
      \q (let [ret -1]
           (s/stop scr)
           ret)
      (recur (getLastKey scr)))
    )
  )

(defn game
  [scr lvl]
  (if (= lvl -1)
    nil
  (loop [input (getLastKey scr)
         mz_data (maze/buildMazeData lvl)
         sc_data (maze/buildScatterMazeData lvl)
         pacman (player. (logic/getPlayerStart sc_data) :stop 0)
         red_ghost (ai/initAi (update (logic/getRedStart sc_data) :y - 2) (get pacman :pos) :scatter :red)
         pink_ghost (ai/initAi (logic/getPinkStart sc_data) (get pacman :pos) :stop :pink)
         blue_ghost (ai/initAi (logic/getBlueStart sc_data) (get pacman :pos) :stop :blue)
         orange_ghost (ai/initAi (logic/getOrangeStart sc_data) (get pacman :pos) :stop :orange)
         move_graph (logic/buildTraversalGraph mz_data)
         scatter_map (logic/buildScatterMap sc_data)
         max_item_count_blue (-> mz_data logic/getEatSet count)
         max_item_count_orange max_item_count_blue
         item_count max_item_count_blue]
    (s/clear scr)
    (s/put-sheet scr 0 0 (maze/mazeSequenceHandler mz_data))
    (s/put-string scr (get (get pacman :pos) :x) (get (get pacman :pos) :y) "☺" {:fg :yellow})
    (drawAI red_ghost scr)
    (drawAI pink_ghost scr)
    (drawAI orange_ghost scr)
    (drawAI blue_ghost scr)
    (s/put-string scr 60 11 (str "Score: " (get pacman :score)) {:fg :white})
    (s/redraw scr)
    (cond (= input \q) (s/stop scr)
          :else (let [new_board (player/update-board-player mz_data pacman)
                      new_pacman (player/update-player-pos mz_data (player/update-player-score mz_data pacman) input)
                      new_red_ghost (ai/schedule-ai (assoc (ai/update-ai red_ghost move_graph scatter_map (get new_pacman :dir) (get red_ghost :pos)) :target (get new_pacman :pos)) (/ 600 1000))
                      new_pink_ghost (ai/schedule-ai (assoc (ai/update-ai pink_ghost move_graph scatter_map (get new_pacman :dir) (get red_ghost :pos)) :target (get new_pacman :pos)) (/ 600 1000))
                      new_blue_ghost (ai/schedule-ai (assoc (ai/update-ai blue_ghost move_graph scatter_map (get new_pacman :dir) (get red_ghost :pos)) :target (get new_pacman :pos)) (/ 600 1000))
                      new_orange_ghost (ai/schedule-ai (assoc (ai/update-ai orange_ghost move_graph scatter_map (get new_pacman :dir) (get red_ghost :pos)) :target (get new_pacman :pos)) (/ 600 1000))]
                  (s/put-string scr 60 11 (str "Score: " (get new_pacman :score)) {:fg :white})
                  (s/redraw scr)
                  (cond (= item_count 0)
                        (do
                          (s/put-string scr 60 12 "You Win!!!!!" {:fg :white :bg :blue})
                          (s/put-string scr 60 13 "Press q to quit or r to replay!" {:bg :red})
                          (s/redraw scr)
                          (loop [opt (getLastKey scr)]
                            (case opt
                              \r (game scr (start scr))
                              \q (s/stop scr)
                              (recur (getLastKey scr)))
                            ))
                        (= ((mz_data (get-in new_pacman [:pos :y])) (get-in new_pacman [:pos :x])) \●)
                        (do (Thread/sleep 600)
                            (recur (getLastKey scr)
                                   new_board
                                   sc_data
                                   new_pacman
                                   (ai/schedule-ai (assoc (ai/update-ai (assoc red_ghost :mode :frightened) move_graph scatter_map (get new_pacman :dir) (get red_ghost :pos)) :target (get new_pacman :pos)) (/ 600 1000))
                                   (ai/schedule-ai (assoc (ai/update-ai (assoc pink_ghost :mode :frightened) move_graph scatter_map (get new_pacman :dir) (get red_ghost :pos)) :target (get new_pacman :pos)) (/ 600 1000))
                                   (ai/schedule-ai (assoc (ai/update-ai (assoc blue_ghost :mode :frightened) move_graph scatter_map (get new_pacman :dir) (get red_ghost :pos)) :target (get new_pacman :pos)) (/ 600 1000))
                                   (ai/schedule-ai (assoc (ai/update-ai (assoc orange_ghost :mode :frightened) move_graph scatter_map (get new_pacman :dir) (get red_ghost :pos)) :target (get new_pacman :pos)) (/ 600 1000))
                                   move_graph
                                   scatter_map
                                   max_item_count_blue
                                   max_item_count_orange
                                   (-> new_board logic/getEatSet count)))
                        (= (get new_red_ghost :pos) (get new_pacman :pos))
                        (cond (= (:mode new_red_ghost) :frightened)
                              (let [new_count (-> new_board logic/getEatSet count)]
                                (Thread/sleep 600)
                                (recur (getLastKey scr)
                                       new_board
                                       sc_data
                                       (update new_pacman :score + 200)
                                       (frightenedReset new_red_ghost (logic/getRedStart sc_data) (:pos new_pacman))
                                       (updateGhostFromStop new_pink_ghost true)
                                       (updateGhostFromStop new_blue_ghost (> (- max_item_count_blue new_count) 30))
                                       (updateGhostFromStop new_orange_ghost (< (/ new_count max_item_count_orange) (/ 2 3)))
                                       move_graph
                                       scatter_map
                                       max_item_count_blue
                                       max_item_count_orange
                                       new_count))
                              :else (do
                                      (s/clear scr)
                                      (s/put-sheet scr 0 0 (maze/mazeSequenceHandler mz_data))
                                      (s/put-string scr (get (get new_pacman :pos) :x) (get (get new_pacman :pos) :y) "☺" {:fg :yellow})
                                      (drawAI red_ghost scr)
                                      (drawAI pink_ghost scr)
                                      (drawAI orange_ghost scr)
                                      (drawAI blue_ghost scr)
                                      (s/put-string scr 60 11 (str "Score: " (get new_pacman :score)) {:fg :white})
                                      (s/put-string scr 60 12 "Game Over!!!!!" {:fg :white :bg :red})
                                      (s/put-string scr 60 13 "Press q to quit or r to replay!" {:bg :red})
                                      (s/redraw scr)
                                      (loop [opt (getLastKey scr)]
                                        (case opt
                                          \r (game scr (start scr))
                                          \q (s/stop scr)
                                          (recur (getLastKey scr)))
                                        ))
                              )
                        (= (get new_pink_ghost :pos) (get new_pacman :pos))
                        (cond (= (:mode new_pink_ghost) :frightened)
                              (let [new_count (-> new_board logic/getEatSet count)]
                                (Thread/sleep 600)
                                (recur (getLastKey scr)
                                       new_board
                                       sc_data
                                       (update new_pacman :score + 200)
                                       (updateGhostFromStop new_red_ghost true)
                                       (frightenedReset new_pink_ghost (logic/getPinkStart sc_data) (:pos new_pacman))
                                       (updateGhostFromStop new_blue_ghost (> (- max_item_count_blue new_count) 30))
                                       (updateGhostFromStop new_orange_ghost (< (/ new_count max_item_count_orange) (/ 2 3)))
                                       move_graph
                                       scatter_map
                                       max_item_count_blue
                                       max_item_count_orange
                                       new_count))
                              :else (do
                                      (s/clear scr)
                                      (s/put-sheet scr 0 0 (maze/mazeSequenceHandler mz_data))
                                      (s/put-string scr (get (get new_pacman :pos) :x) (get (get new_pacman :pos) :y) "☺" {:fg :yellow})
                                      (drawAI red_ghost scr)
                                      (drawAI pink_ghost scr)
                                      (drawAI orange_ghost scr)
                                      (drawAI blue_ghost scr)
                                      (s/put-string scr 60 11 (str "Score: " (get new_pacman :score)) {:fg :white})
                                      (s/put-string scr 60 12 "Game Over!!!!!" {:fg :white :bg :red})
                                      (s/put-string scr 60 13 "Press q to quit or r to replay!" {:bg :red})
                                      (s/redraw scr)
                                      (loop [opt (getLastKey scr)]
                                        (case opt
                                          \r (game scr (start scr))
                                          \q (s/stop scr)
                                          (recur (getLastKey scr)))
                                        ))
                              )
                        (= (get new_blue_ghost :pos) (get new_pacman :pos))
                        (cond (= (:mode new_blue_ghost) :frightened)
                              (let [new_count (-> new_board logic/getEatSet count)]
                                (Thread/sleep 600)
                                (recur (getLastKey scr)
                                       new_board
                                       sc_data
                                       (update new_pacman :score + 200)
                                       (updateGhostFromStop new_red_ghost true)
                                       (updateGhostFromStop new_pink_ghost true)
                                       (frightenedReset new_blue_ghost (logic/getBlueStart sc_data) (:pos new_pacman))
                                       (updateGhostFromStop new_orange_ghost (< (/ new_count max_item_count_orange) (/ 2 3)))
                                       move_graph
                                       scatter_map
                                       new_count
                                       max_item_count_orange
                                       new_count))
                              :else (do
                                      (s/clear scr)
                                      (s/put-sheet scr 0 0 (maze/mazeSequenceHandler mz_data))
                                      (s/put-string scr (get (get new_pacman :pos) :x) (get (get new_pacman :pos) :y) "☺" {:fg :yellow})
                                      (drawAI red_ghost scr)
                                      (drawAI pink_ghost scr)
                                      (drawAI orange_ghost scr)
                                      (drawAI blue_ghost scr)
                                      (s/put-string scr 60 11 (str "Score: " (get new_pacman :score)) {:fg :white})
                                      (s/put-string scr 60 12 "Game Over!!!!!" {:fg :white :bg :red})
                                      (s/put-string scr 60 13 "Press q to quit or r to replay!" {:bg :red})
                                      (s/redraw scr)
                                      (loop [opt (getLastKey scr)]
                                        (case opt
                                          \r (game scr (start scr))
                                          \q (s/stop scr)
                                          (recur (getLastKey scr)))
                                        ))
                              )
                        (= (get new_orange_ghost :pos) (get new_pacman :pos))
                        (cond (= (:mode new_orange_ghost) :frightened)
                              (let [new_count (-> new_board logic/getEatSet count)]
                                (Thread/sleep 600)
                                (recur (getLastKey scr)
                                       new_board
                                       sc_data
                                       (update new_pacman :score + 200)
                                       (updateGhostFromStop new_red_ghost true)
                                       (updateGhostFromStop new_pink_ghost true)
                                       (updateGhostFromStop new_blue_ghost (> (- max_item_count_blue new_count) 30))
                                       (frightenedReset new_orange_ghost (logic/getOrangeStart sc_data) (:pos new_pacman))
                                       move_graph
                                       scatter_map
                                       max_item_count_blue
                                       new_count
                                       new_count))
                              :else (do
                                      (s/clear scr)
                                      (s/put-sheet scr 0 0 (maze/mazeSequenceHandler mz_data))
                                      (s/put-string scr (get (get new_pacman :pos) :x) (get (get new_pacman :pos) :y) "☺" {:fg :yellow})
                                      (drawAI red_ghost scr)
                                      (drawAI pink_ghost scr)
                                      (drawAI orange_ghost scr)
                                      (drawAI blue_ghost scr)
                                      (s/put-string scr 60 11 (str "Score: " (get new_pacman :score)) {:fg :white})
                                      (s/put-string scr 60 12 "Game Over!!!!!" {:fg :white :bg :red})
                                      (s/put-string scr 60 13 "Press q to quit or r to replay!" {:bg :red})
                                      (s/redraw scr)
                                      (loop [opt (getLastKey scr)]
                                        (case opt
                                          \r (game scr (start scr))
                                          \q (s/stop scr)
                                          (recur (getLastKey scr)))
                                        ))
                              )
                        (and (= (get new_red_ghost :pos) (get pacman :pos)) (isOpposite (get pacman :dir) (ai/get-ai-dir new_red_ghost)))
                        (cond (= (:mode new_red_ghost) :frightened)
                              (let [new_count (-> new_board logic/getEatSet count)]
                                (Thread/sleep 600)
                                (recur (getLastKey scr)
                                       new_board
                                       sc_data
                                       (update new_pacman :score + 200)
                                       (frightenedReset new_red_ghost (logic/getRedStart sc_data) (:pos new_pacman))
                                       (updateGhostFromStop new_pink_ghost true)
                                       (updateGhostFromStop new_blue_ghost (> (- max_item_count_blue new_count) 30))
                                       (updateGhostFromStop new_orange_ghost (< (/ new_count max_item_count_orange) (/ 2 3)))
                                       move_graph
                                       scatter_map
                                       max_item_count_blue
                                       max_item_count_orange
                                       new_count))
                              :else (do
                                      (s/put-string scr 60 11 (str "Score: " (get new_pacman :score)) {:fg :white})
                                      (s/put-string scr 60 12 "Game Over!!!!!" {:fg :white :bg :red})
                                      (s/put-string scr 60 13 "Press q to quit or r to replay!" {:bg :red})
                                      (s/redraw scr)
                                      (loop [opt (getLastKey scr)]
                                        (case opt
                                          \r (game scr (start scr))
                                          \q (s/stop scr)
                                          (recur (getLastKey scr)))
                                        ))
                              )
                        (and (= (get new_pink_ghost :pos) (get pacman :pos)) (isOpposite (get pacman :dir) (ai/get-ai-dir new_pink_ghost)))
                        (cond (= (:mode new_pink_ghost) :frightened)
                              (let [new_count (-> new_board logic/getEatSet count)]
                                (Thread/sleep 600)
                                (recur (getLastKey scr)
                                       new_board
                                       sc_data
                                       (update new_pacman :score + 200)
                                       (updateGhostFromStop new_red_ghost true)
                                       (frightenedReset new_pink_ghost (logic/getPinkStart sc_data) (:pos new_pacman))
                                       (updateGhostFromStop new_blue_ghost (> (- max_item_count_blue new_count) 30))
                                       (updateGhostFromStop new_orange_ghost (< (/ new_count max_item_count_orange) (/ 2 3)))
                                       move_graph
                                       scatter_map
                                       max_item_count_blue
                                       max_item_count_orange
                                       new_count))
                              :else (do
                                      (s/put-string scr 60 11 (str "Score: " (get new_pacman :score)) {:fg :white})
                                      (s/put-string scr 60 12 "Game Over!!!!!" {:fg :white :bg :red})
                                      (s/put-string scr 60 13 "Press q to quit or r to replay!" {:bg :red})
                                      (s/redraw scr)
                                      (loop [opt (getLastKey scr)]
                                        (case opt
                                          \r (game scr (start scr))
                                          \q (s/stop scr)
                                          (recur (getLastKey scr)))
                                        ))

                              )
                        (and (= (get new_blue_ghost :pos) (get pacman :pos)) (isOpposite (get pacman :dir) (ai/get-ai-dir new_blue_ghost)))
                        (cond (= (:mode new_blue_ghost) :frightened)
                              (let [new_count (-> new_board logic/getEatSet count)]
                                (Thread/sleep 600)
                                (recur (getLastKey scr)
                                       new_board
                                       sc_data
                                       (update new_pacman :score + 200)
                                       (updateGhostFromStop new_red_ghost true)
                                       (updateGhostFromStop new_pink_ghost true)
                                       (frightenedReset new_blue_ghost (logic/getBlueStart sc_data) (:pos new_pacman))
                                       (updateGhostFromStop new_orange_ghost (< (/ new_count max_item_count_orange) (/ 2 3)))
                                       move_graph
                                       scatter_map
                                       new_count
                                       max_item_count_orange
                                       new_count))
                              :else (do
                                      (s/put-string scr 60 11 (str "Score: " (get new_pacman :score)) {:fg :white})
                                      (s/put-string scr 60 12 "Game Over!!!!!" {:fg :white :bg :red})
                                      (s/put-string scr 60 13 "Press q to quit or r to replay!" {:bg :red})
                                      (s/redraw scr)
                                      (loop [opt (getLastKey scr)]
                                        (case opt
                                          \r (game scr (start scr))
                                          \q (s/stop scr)
                                          (recur (getLastKey scr)))
                                        ))
                              )
                        (and (= (get new_orange_ghost :pos) (get pacman :pos)) (isOpposite (get pacman :dir) (ai/get-ai-dir new_orange_ghost)))
                        (cond (= (:mode new_orange_ghost) :frightened)
                              (let [new_count (-> new_board logic/getEatSet count)]
                                (Thread/sleep 600)
                                (recur (getLastKey scr)
                                       new_board
                                       sc_data
                                       (update new_pacman :score + 200)
                                       (updateGhostFromStop new_red_ghost true)
                                       (updateGhostFromStop new_pink_ghost true)
                                       (updateGhostFromStop new_blue_ghost (> (- max_item_count_blue new_count) 30))
                                       (frightenedReset new_orange_ghost (logic/getOrangeStart sc_data) (:pos new_pacman))
                                       move_graph
                                       scatter_map
                                       max_item_count_blue
                                       new_count
                                       new_count))
                              :else (do
                                      (s/put-string scr 60 11 (str "Score: " (get new_pacman :score)) {:fg :white})
                                      (s/put-string scr 60 12 "Game Over!!!!!" {:fg :white :bg :red})
                                      (s/put-string scr 60 13 "Press q to quit or r to replay!" {:bg :red})
                                      (s/redraw scr)
                                      (loop [opt (getLastKey scr)]
                                        (case opt
                                          \r (game scr (start scr))
                                          \q (s/stop scr)
                                          (recur (getLastKey scr)))
                                        ))
                              )
                        :else (let [new_count (-> new_board logic/getEatSet count)]
                                (Thread/sleep 600)
                                (recur (getLastKey scr)
                                       new_board
                                       sc_data
                                       new_pacman
                                       (updateGhostFromStop new_red_ghost true)
                                       (updateGhostFromStop new_pink_ghost true)
                                       (updateGhostFromStop new_blue_ghost (> (- max_item_count_blue new_count) 30))
                                       (updateGhostFromStop new_orange_ghost (< (/ new_count max_item_count_orange) (/ 2 3)))
                                       move_graph
                                       scatter_map
                                       max_item_count_blue
                                       max_item_count_orange
                                       new_count))
                        )
                  ))
    )))

(defn -main
  [& args]
  (let [game_scr (if (or (-> args first (= "-t")) (-> args first (= args first "--text"))) (s/get-screen :text) (s/get-screen :swing))]
    (s/start game_scr)
    (game game_scr (start game_scr))))
