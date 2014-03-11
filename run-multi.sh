#!/bin/bash

if [ -z $2 ]; then
    echo "Usage: ./run-multi.sh <player_name> <server_ip>"
    exit 1;
fi

./compile.sh && java \
    -Djava.library.path=lib/native \
    -Dcargame.multiplayer_mode=true \
    -Dcargame.host_name=$2 \
    -Dcargame.player_name=$1 \
    -classpath lib/lwjgl.jar:lib/slick.jar:src:lib/kryonet-1.04-all.jar \
    org.cargame.CarGame
