@echo off
REM ============================================================
REM  TecnoMovil Data - compilar y ejecutar en Windows
REM  Requisito: tener Java (JDK 17 o superior) instalado.
REM  Uso: doble clic en este archivo, o desde la consola:
REM       ejecutar.bat            (150.000 usuarios)
REM       ejecutar.bat 300000     (otra cantidad de usuarios)
REM ============================================================
cd /d "%~dp0"
if exist out rmdir /s /q out
echo Compilando...
javac -encoding UTF-8 -d out src\tecnomovil\*.java src\tecnomovil\modelo\*.java src\tecnomovil\datos\*.java src\tecnomovil\procesamiento\*.java src\tecnomovil\reportes\*.java
if errorlevel 1 (
  echo Error al compilar. Revise que Java este instalado: javac -version
  pause
  exit /b 1
)
echo.
java -cp out tecnomovil.Verificacion
echo.
java -cp out tecnomovil.App %1
echo.
pause
