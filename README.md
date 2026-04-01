# Weather App + AI

Aplicación del clima con integración de IA (DeepSeek) para recomendaciones vestimentarias.

## Características

- Consultar clima actual de cualquier ciudad (API Open-Meteo)
- Pronóstico de 5 días
- Recomendaciones de IA sobre qué ropa usar
- Interfaz gráfica (Swing) o modo consola
- Soporta español

## Requisitos

- Java 21+
- Maven 3.8+

## Configuración

1. Crear archivo `.env` con tu API key de DeepSeek:

```bash
DEEPSEEK_API_KEY=tu_api_key_aqui
```

O usar variable de entorno `DEEPSEEK_API_KEY`.

## Ejecutar

```bash
# Modo gráfico
mvn exec:java -Dexec.mainClass="weatherapp.Main"

# Modo consola (servidor sin GUI)
mvn exec:java -Dexec.mainClass="weatherapp.Main" -Dexec.args="console"
```

## Tests

```bash
mvn test
```

## Tecnologías

- Java 21 (records, HttpClient)
- Gson para JSON
- Open-Meteo API
- DeepSeek Chat API