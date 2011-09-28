#!/bin/bash

javac -cp lib/lwjgl.jar:lib/slick.jar:lib/JGN_20110725.jar \
    src/org/cargame/*.java  \
    src/org/cargame/server/*.java 
