#!/bin/sh
# TecnoMovil Data - compilar y ejecutar en Linux o Mac
# Uso: ./ejecutar.sh  o  ./ejecutar.sh 300000
cd "$(dirname "$0")"
rm -rf out
javac -encoding UTF-8 -d out $(find src -name "*.java") || exit 1
java -cp out tecnomovil.Verificacion && echo && java -cp out tecnomovil.App "$@"
