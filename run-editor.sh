#!/bin/bash

./compile.sh && java \
    -Djava.library.path=lib/native \
    -classpath lib/lwjgl.jar:lib/slick.jar:src:lib/kryonet-1.04-all.jar \
    org.cargame.editor.Editor
