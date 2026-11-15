@echo off
REM Usage: run.bat test   -> compile and run the tests
REM        run.bat        -> compile and start the server on port 8080
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
del sources.txt
if "%1"=="test" (
  java -cp out com.vinamra.payflow.TestRunner
) else (
  java -cp out com.vinamra.payflow.Main %*
)
