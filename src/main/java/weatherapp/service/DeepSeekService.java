package weatherapp.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import weatherapp.model.WeatherData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DeepSeekService {

    private static final String API_KEY = loadApiKey();
    private static final String BASE_URL = "https://api.deepseek.com/chat/completions";

    private static String loadApiKey() {
        // 1. Intentar variables de entorno nativas (recomendado)
        String key = System.getenv("DEEPSEEK_API_KEY");
        if (key != null && !key.isBlank()) return key;

        // 2. Si no existe, intentar leer desde el archivo local .env
        File envFile = new File(".env");
        if (!envFile.exists()) return null;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Soportar varios formatos comunes
                if (line.trim().startsWith("API_KEY:") || line.trim().startsWith("API_KEY=")) {
                    return line.split("[:=]", 2)[1].trim();
                } else if (line.trim().startsWith("DEEPSEEK_API_KEY=") || line.trim().startsWith("DEEPSEEK_API_KEY:")) {
                    return line.split("[:=]", 2)[1].trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Advertencia: No se pudo leer el archivo .env");
        }
        return null;
    }

    public void analyzeWeather(WeatherData weatherData, String userQuestion) throws Exception {
        if (API_KEY == null || API_KEY.isBlank()) {
            weatherData.setAiRecommendation("Error: Falta la llave de acceso (DEEPSEEK_API_KEY) o el archivo .env no tiene la configuración correcta.");
            return;
        }

        // Construir el prompt restrictivo para la IA
        StringBuilder prompt = new StringBuilder();
        prompt.append("ERES UN ESTRICTO ASISTENTE METEOROLÓGICO. ");
        prompt.append("REGLA DE ORO: Tienes ABSOLUTAMENTE PROHIBIDO responder o hablar sobre temas ajenos al clima, la temperatura, condiciones del cielo o tipos de ropa sugerida. Si el usuario te pregunta por código de programación, historia, política, o cualquier otra cosa, DEBES NEGARTE amablemente recordando tu rol de meteorólogo.\n\n");
        prompt.append("Datos Climatológicos Actuales:\n");
        prompt.append("- Clima: ").append(weatherData.getDescription()).append("\n");
        prompt.append("- Temperatura actual: ").append(weatherData.getTemperature()).append(" C\n");
        prompt.append("- Humedad: ").append(weatherData.getHumidity()).append("%\n");
        prompt.append("- Pronóstico mínimas futuras: ").append(weatherData.getUpcomingMinTemps()).append("\n");
        prompt.append("- Pronóstico máximas futuras: ").append(weatherData.getUpcomingMaxTemps()).append("\n\n");

        if (userQuestion != null && !userQuestion.isEmpty()) {
            prompt.append("PREGUNTA ESPECÍFICA DEL USUARIO: '").append(userQuestion).append("'. ");
            prompt.append("Basa tu respuesta en los datos dados. No excedas las 3 o 4 oraciones.");
        } else {
            prompt.append("Da una recomendación general de 3 oraciones cortas sobre qué ropa usar y la tendencia proyectada del clima.");
        }

        // Para inyectar el prompt de manera segura en un block JSON, escapamos comillas y saltos de línea:
        String safePrompt = prompt.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
        
        String jsonBody = """
            {
                "model": "deepseek-chat",
                "messages": [
                    {"role": "user", "content": "%s"}
                ],
                "temperature": 0.7,
                "max_tokens": 200
            }
            """.formatted(safePrompt);

        // Cliente HTTP Moderno de Java 11+
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String errorDetail = "";
                try {
                    JsonObject errJson = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (errJson.has("error")) {
                        errorDetail = errJson.getAsJsonObject("error").get("message").getAsString();
                    }
                } catch (Exception ex) {
                    errorDetail = response.body();
                }

                if (response.statusCode() == 402) {
                    weatherData.setAiRecommendation("Error 402 (Saldo Insuficiente): Tu cuenta de DeepSeek agotó sus créditos. \n⚠️ Detalle de API: " + errorDetail);
                } else if (response.statusCode() == 401) {
                    weatherData.setAiRecommendation("Error 401 (No Autorizado): Tu llave secreta API_KEY fue rechazada. \n⚠️ Detalle de API: " + errorDetail);
                } else {
                    weatherData.setAiRecommendation("Error HTTP " + response.statusCode() + ": \n⚠️ Detalle: " + errorDetail);
                }
                return;
            }

            // Parsear la respuesta JSON moderna
            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
            String aiResponseText = responseJson.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            weatherData.setAiRecommendation(aiResponseText.trim());

        } catch (Exception e) {
            weatherData.setAiRecommendation("No se pudo conectar con la IA: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
