README
=====

CarGame v0.1 ***ALPHA***

CarGame is a fast-paced, multiplayer deathmatch game where you try to run over your friends with hovercrafts.

In CarGame, _speed_is_king_ and the slower of any two cars that touch EXPLODES!

Getting Started
===============

* To start a server:
  * Run CarGameServer.bat
  * You will need to open port 30241 for TCP and 30341 for UDP if firewalled

* To start the client and connect to a server:
  * Edit CarGame.bat and change -Dcargame.player_name="Player" to your preference
  * If necessary, change cargame.host_name="intis.dyndns.org" to point to the server
  * Run CarGame.bat

Client Controls
===============

* W,A,S,D moves your hovercraft up, left, down, right respectively
* Move mouse cursor to point your hovercraft
* Left mouse click activates BOOST, which greatly accelerates you in the direction you are facing
  * BOOST has a 2.5s cooldown
* Q activates JAMMER, which makes your vehicle more difficult to see and prevents others from knowing your
  location.
  * JAMMER has a 10s duration and a 30s cooldown
  
* Enter to chat/send remote admin commands
* F1 toggles fullscreen mode
* F2 toggles mute

TODO
====

* Level editor
* Chat console [DONE!]
  * Remote server administration through chat console [DONE!]

