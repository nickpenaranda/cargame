#!/bin/bash

./compile.sh && java -Djava.library.path=lib/native \
    -classpath lib/lwjgl.jar:lib/slick.jar:src:lib/JGN_20110725.jar \
    org.cargame.server.Server
