# Pacman Adventure Game

A text-based version of Pacman designed with Clojure that tries to replicate the orginal AI behavior of Pacman's original Ghost Characters

## Usage

```
# To run the swing console, just run the command below
lein run

# However, if you want to run the game in your terminal
lein run --text
# or
lein run -t
```

If you want to produce a standalone Jar, run the following command.
```
lein uberjar
```

Once the standalone Jar has been produced (should have standalone in the name),
```
java -jar <name of jar>-standalone.jar

# If wanting to use text mode
java -jar <name of jar>-standalone.jar --text
# or
java -jar <name of jar>.jar -t
```

## Base Overview

Currently have four game AI, types 13 maps, and scoring/end game mechanisms.

## Details Behind AI Development

The reason I chose Pacman as my project is that Pacman's ghost AIs are very diverse and emulating the original behavior can be tricky. I thought because of this aspect, Pacman would be an ideal project to practition my new knowledge in Clojure. That, and I don't like to do projects that are similar to others (I am more of a free spirit :).

### General AI Modes

* :scatter - The scatter modes is cycling mode that has the AIs cycle four corner-ish areas of the map. Doing so prevents them from outright destroying Pacman. In this mode, the ghosts do not specifically target Pacman, but instead go after tracers embedded in a customized version of each map known as scatter_mazes.
* :chase - The ghosts have had enough rest and now they are ready to go after Pacman. Each ghost will target Pacman differently based on their attributes which will be discussed further below. One warning: If all ghosts are in chase mode, good luck surviving. You are going to need to frighten them to live.
* :frightened - This mode is triggered when Pacman eats the larger dot. This mode enables Pacman to eat the ghosts. The ghosts moverment behavior become pseudorandom at this point. If eaten by Pacman, they are reset.

Original Intervals for Pacman Modes (After Each Ghost has begun moving)
1. Scatter for 7 seconds then chase for 20 seconds
2. Scatter for 7 seconds then chase for 20 seconds
3. Scatter for 5 seconds then chase for 20 seconds
4. Scatter for 5 seconds then chase for the remaining duration

Modified Intervals to Account for Graphics Delay Due to Lack of Smooth Transitions (due to non-pixel utf-8 character based rendering mechanism) / (Since you only have one life, increased scatter duration ;)
1. Scatter for 25 seconds then chase for 30 seconds
2. Scatter for 25 seconds then chase for 30 seconds
3. Scatter for 20 seconds then chare for 30 seconds
4. Scatter for 20 seconds then chase indefinitely

Original frightened period was 8 seconds. Clojure version is 15 seconds.

### Blinky (Red Ghost) - Japanese version (追いかけ, oikake), meaning pursuer/chaser

Blinky, the red ghost of Pacman, might have the most simple characteristic (he goes staright for the kill and goes right after you), but his implementation could not be more elegant. To implement his hunting skills, I was inspired by our MP7 (a maze building/path finding MP using graphs) in CS 225, so I decided to use graphs for my AI to represent the maps and determine the shortest path to the target, in the case of Blinky, Pacman. And what could be better than MP7, doing MP7 in Clojure.

![Image of Blinky](http://media.gameinternals.com/pacman-ghosts/blinky-targeting.png) Example of Blinky's targetting method in chase mode as he heads straight for PacMan

### Pinky (Pink Ghost) - Japanese version (待ち伏せ, machibuse), meaning ambusher
Now, Pinky's AI characteristic is quite interesting in the fact that it uses Blinky's targeting strategy except it targets the square four spaces in front of the direction Pacman is traveling in. However, even this rule has a few exceptions, one being that when Pacman travels upward, Pink actually targets 4 spaces above Pacman and 4 spaces to the left of Pacman. In the original Pacman game, this behavior was not actually intended but was a result of an unintended overflow error in the programming.


Assembly for controlling targetting behavior for Pinky
```
; Original Z80 Assembly code
; (#4D39) is the memory address location that possesses the position vector data for PacMan
; Upper byte contains X data, lower byte contains Y data
; (#4D1C) stores the direction vector for Pacman (Upper byte is for X data, lower byte for Y data)
LD DE, (#4D39)
LD HL, (#4D1C)
ADD HL, HL  ; Double direction value
ADD HL, HL  ; Quadruple direction value
ADD HL, DE  ; Now take offset vector and use it to compute target position based on PacMan position
```

Now, lets run through an example as to why the above assembly code causes an overflow when handling the upward direction vector.

In their setup, #00FF is mapped to (1,-1) as we all know adding 0 + 0 = 0, so surely simply adding #00FF with itself would just leave the zeros as is. It would work for some (not even all) trivial cases, but clearly the programmer working on this did not bother to write good test cases / use gdb to check the state logic of the game, because the engineer working on this noticed that when adding FF to itself it will result in FE which indeed -2 but missed an important point. There is also a carry over bit from this operation, resulting in the final output becoming #01FE (2,-2) instead of the desired #00FE (1, -2). So, in the next doubling operation #01FE (2,-2) will result in #03FC (4,-4) because #01 becomes #02 and there is an overflow bit from the addition of #FE with itself.

Example of how to fix the bug (Made this up myself, so feel free to correct me)
```
; Modified Z80 code
; Also, it may seem like the bug is simple, but in reality its very tricky to think about/resolve
; considering how the Z80 assembly language is structured.
LD HL, (#4D1C)
LD B, H
ADD B, B
ADD B, B
LD C, L
ADD C, C
ADD C, C
LD DE, (#4D39)
ADD DE, BC ; This code produces the correct value in register DE for the target position
```

However, I am very happy about the fact that this bug exists in PacMan. I think it gives Pinky character, and I have reimplemented Pinky's behavior, including this bug, in my Clojure implementation of PacMan. So what does Pinky's behavior mean for PacMan. It means that PacMan can in theory push back Pinky. But be warned, when Pinky is cornered, you will know who the real cat and mouse of this little chase are. Unfortuantely, when Pinky is pushed back, I did not implement the unique cycling behavior Pinky had, but instead had him push back and spring forth an attack when cornered, resulting in a similar strategy in the game. In my next revision, I hope to implement Pinky's cycling behavior when pushed back.

Example of Pinky's targetting ![Image of Pinky](http://media.gameinternals.com/pacman-ghosts/pinky-targeting.png)

![Pinky Glitch](http://media.gameinternals.com/pacman-ghosts/pinky-targeting2.png) Glitch with Pinky's targetting when PacMan moves upwards

Pinky pushed back ![Pushed Back](http://media.gameinternals.com/pacman-ghosts/pinky-chicken.png)

### Inky (Blue Ghost) - Japanese version (気紛れ, kimagure), meaning whimsical

Inky is truly a whimsical character. Inky will only start moving after at least 30 food items have been eaten. His target is calculated by using the direction vector for Blinky (Red Ghost) towards Pacman, then using that vector to calculate the target based off of PacMan's position data. Just like the original, the Clojure implementation reimplements this behavior for the Blue Ghost. However, few minor tweaks can be done to improve its quality. Overall though, pretty much consistent with the original.

![Inky Targetting](http://media.gameinternals.com/pacman-ghosts/inky-targeting.png) Example of Inky's targetting

### Clyde (Orange Ghost) - Japanese version (お惚け, otoboke), meaning feining ignorance

Clyde is really fascinating. At first glance, someone would assume his behavior was pseudorandom but actually it is very distinct. If PacMan is more than 8 tile space distance away, Clyde will attack like Blinky (Red Ghost). However, if Pacman is less than 8 tile spaces away, Clyde will go into its scatter mode.

The Clojure implementation has Clydes behavior working, however, because of the hysterosis, transitions may not be as smooth as the original game. Todo, would be to build transitionary states for Clyde to improve smoothness.

Examples of Clyde's Targetting Behavior

![Base Target Mode](http://media.gameinternals.com/pacman-ghosts/clyde-targeting2.png) ![Base Target Mode](http://media.gameinternals.com/pacman-ghosts/clyde-targeting.png) ![Proximity Target](http://media.gameinternals.com/pacman-ghosts/clyde-targeting3.png)

### Acknowledgements for AI Information, Images, and Resources

My understanding of the AI behavior and pictures used are from [this resource](http://gameinternals.com/post/2072558330/understanding-pac-man-ghost-behavior).
For my graphs, I utilized the [Uber library](https://github.com/Engelberg/ubergraph).

## Map Data

The map data used in this game is a modified version from  the following link.
Only the map data was based from this link. None of the game logic/AI code, etc.

[Map Resource For Pacman](https://github.com/pac1979/PacMan/blob/master/pacman.clj)

## Images of Current Gameplay

![Initial Screen](https://d1b10bmlvqabco.cloudfront.net/attach/j7wjr524azc36i/ixkys93us76yg/jbkqmj4xbxc/241217060830.png)

![In-Play](https://d1b10bmlvqabco.cloudfront.net/attach/j7wjr524azc36i/ixkys93us76yg/jbkqnzjzn34b/241217061706.png)

![Game Over](https://d1b10bmlvqabco.cloudfront.net/attach/j7wjr524azc36i/ixkys93us76yg/jbkqocc78j7z/241217061638.png)

## TODO for v2

* Add audio
* Get timings to match the original game
* Add smooth transitions and upgrade AI characteristics to be even more authentic
* Power logic using macros in Clojure

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
