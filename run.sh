#!/bin/bash

./compile.sh && java \
    -Djava.library.path=lib/native \
    -Dcargame.multiplayer_mode=false \
    -classpath lib/lwjgl.jar:lib/slick.jar:src:lib/JGN_20110725.jar \
    org.cargame.CarGame
