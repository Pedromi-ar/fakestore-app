#!/usr/bin/env bash
set -e
mkdir -p bin
javac -encoding UTF-8 -d bin $(find src -name "*.java")
java -cp bin com.tienda.Main
