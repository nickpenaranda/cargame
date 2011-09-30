#!/bin/bash

./compile.sh && java \
    -Djava.library.path=lib/native \
    -Dcargame.multiplayer_mode=false \
    -classpath lib/lwjgl.jar:lib/slick.jar:src:lib/kryonet-1.04-all.jar \
    org.cargame.CarGame
