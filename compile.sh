#!/bin/bash

javac -cp lib/lwjgl.jar:lib/slick.jar:lib/kryonet-1.04-all.jar \
    src/org/cargame/*.java  \
    src/org/cargame/server/*.java 
