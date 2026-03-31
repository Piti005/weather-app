FROM maven:3.9.6-eclipse-temurin-21

# Evitar prompts de terminal interactivos durante la instalación de paquetes
ENV DEBIAN_FRONTEND=noninteractive

# Instalar dependencias necesarias para emular el monitor y web
# xvfb: Pantalla falsa
# x11vnc: Grabador de pantalla
# novnc: Transmisor a web (html5)
# fluxbox: Para poder arrastrar la ventana y cerrarla con la "X"
RUN apt-get update && apt-get install -y \
    xvfb \
    x11vnc \
    novnc \
    net-tools \
    fluxbox \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Optimización de caché de Docker: Descargar las librerías Java primero
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiar código y configuraciones (.env para la clave)
COPY src ./src
COPY .env ./

# Configurar Script de inicialización
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Exponer el puerto donde NoVNC abrirá la página web gráfica
EXPOSE 8080

# Lanzar todo nuestro stack de arranque cuando inicie el contenedor
CMD ["/entrypoint.sh"]
