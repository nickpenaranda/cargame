#!/bin/bash

./compile.sh && java \
    -Djava.library.path=lib/native \
    -Dcargame.multiplayer_mode=true \
    -Dcargame.host_name=76.21.9.224 \
    -Dcargame.player_name=shawn \
    -classpath lib/lwjgl.jar:lib/slick.jar:src:lib/kryonet-1.04-all.jar \
    org.cargame.CarGame
