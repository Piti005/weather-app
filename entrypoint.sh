#!/bin/bash
echo "Iniciando monitor virtual Xvfb..."
export DISPLAY=:99
Xvfb :99 -screen 0 1024x768x24 &
sleep 2

echo "Iniciando gestor de ventanas Fluxbox (para enmarcar la app)..."
fluxbox &
sleep 1

echo "Iniciando servidor VNC oculto..."
x11vnc -display :99 -forever -nopw -quiet -listen localhost -xkb &
sleep 2

websockify --web /usr/share/novnc 8080 localhost:5900 &

echo "=========================================================="
echo "          COMPILANDO Y LANZANDO LA APLICACIÓN             "
echo "  Ingresa desde tu navegador a http://localhost:8080      "
echo "=========================================================="

cd /app
# Cargamos la variable DeepSeek desde el archivo .env oculto de manera automática 
# y a su vez ejecutamos maven forzandolo a correr sobre la DISPLAY:99
mvn compile exec:java -q

# Mantener vivo el contendor si la aplicación se llega a cerrar manuamente
sleep infinity
