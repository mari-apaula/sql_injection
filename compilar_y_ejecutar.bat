@echo off
REM Script para Windows
echo Compilando...
javac -cp sqlite-jdbc-3.46.1.3.jar CursosDemo.java
if errorlevel 1 exit /b 1
echo Ejecutando...
java -cp .;sqlite-jdbc-3.46.1.3.jar CursosDemo
pause
