#!/usr/bin/env bash
# Usage: ./run.sh test   -> compile and run the tests
#        ./run.sh        -> compile and start the server on port 8080 (or ./run.sh 9090)
set -e
rm -rf out && mkdir out
javac -d out $(find src -name "*.java")
if [ "$1" = "test" ]; then
  java -cp out com.vinamra.payflow.TestRunner
else
  java -cp out com.vinamra.payflow.Main "$@"
fi
