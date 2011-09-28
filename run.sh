#!/bin/bash

./compile.sh && java -Djava.library.path=lib/native \
    -classpath lib/lwjgl.jar:lib/slick.jar:src \
    org.cargame.CarGame
