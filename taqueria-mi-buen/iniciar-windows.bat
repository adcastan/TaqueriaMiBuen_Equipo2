@echo off
cd /d "%~dp0"
echo MI BUEN - ASADA POS - Practica local
echo Abre http://127.0.0.1:8787 en tu navegador cuando aparezca la estacion.
echo Deja esta ventana abierta. Para detener usa Ctrl+C.
where py >nul 2>nul
if errorlevel 1 goto usar_python
py -3 server.py
goto terminar
:usar_python
python server.py
:terminar
pause
