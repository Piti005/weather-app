package weatherapp.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.Map;

public class DeepSeekService {

    private static final String API_KEY = loadApiKey();
    private static final String BASE_URL = "https://api.deepseek.com/chat/completions";
    private static final int TIMEOUT_SECONDS = 30;

    private final HttpClient httpClient;
    private final Gson gson;

    public DeepSeekService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.gson = new GsonBuilder().create();
    }

    private static String loadApiKey() {
        String key = System.getenv("DEEPSEEK_API_KEY");
        if (key != null && !key.isBlank()) return key;

        File envFile = new File(".env");
        if (!envFile.exists()) return null;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("API_KEY:") || trimmed.startsWith("API_KEY=")) {
                    return line.split("[:=]", 2)[1].trim();
                } else if (trimmed.startsWith("DEEPSEEK_API_KEY=") || trimmed.startsWith("DEEPSEEK_API_KEY:")) {
                    return line.split("[:=]", 2)[1].trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Advertencia: No se pudo leer el archivo .env");
        }
        return null;
    }

    public WeatherData analyzeWeather(WeatherData weatherData, String userQuestion) {
        if (API_KEY == null || API_KEY.isBlank()) {
            return weatherData.withAiRecommendation("Error: Falta la llave de acceso (DEEPSEEK_API_KEY) o el archivo .env no tiene la configuración correcta.");
        }

        String prompt = buildPrompt(weatherData, userQuestion);
        String jsonBody = gson.toJson(Map.of(
            "model", "deepseek-chat",
            "messages", new Object[] { Map.of("role", "user", "content", prompt) },
            "temperature", 0.7,
            "max_tokens", 200
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String errorDetail = extractErrorDetail(response.body());
                String errorMsg = switch (response.statusCode()) {
                    case 402 -> "Error 402 (Saldo Insuficiente): Tu cuenta de DeepSeek agotó sus créditos.";
                    case 401 -> "Error 401 (No Autorizado): Tu llave secreta API_KEY fue rechazada.";
                    default -> "Error HTTP " + response.statusCode();
                };
                return weatherData.withAiRecommendation(errorMsg + "\n⚠️ Detalle: " + errorDetail);
            }

            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
            String aiResponseText = responseJson.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            return weatherData.withAiRecommendation(aiResponseText.trim());

        } catch (Exception e) {
            return weatherData.withAiRecommendation("No se pudo conectar con la IA: " + e.getMessage());
        }
    }

    private String buildPrompt(WeatherData data, String userQuestion) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("ERES UN ESTRICTO ASISTENTE METEOROLÓGICO. ");
        prompt.append("REGLA DE ORO: Tienes ABSOLUTAMENTE PROHIBIDO responder o hablar sobre temas ajenos al clima, la temperatura, condiciones del cielo o tipos de ropa sugerida. Si el usuario te pregunta por código de programación, historia, política, o cualquier otra cosa, DEBES NEGARTE amablemente recordando tu rol de meteorólogo.\n\n");
        prompt.append("Datos Climatológicos Actuales:\n");
        prompt.append("- Clima: ").append(data.description()).append("\n");
        prompt.append("- Temperatura actual: ").append(data.temperature()).append(" C\n");
        prompt.append("- Humedad: ").append(data.humidity()).append("%\n");
        prompt.append("- Pronóstico mínimas futuras: ").append(data.upcomingMinTemps()).append("\n");
        prompt.append("- Pronóstico máximas futuras: ").append(data.upcomingMaxTemps()).append("\n\n");

        if (userQuestion != null && !userQuestion.isEmpty()) {
            prompt.append("PREGUNTA ESPECÍFICA DEL USUARIO: '").append(userQuestion).append("'. ");
            prompt.append("Basa tu respuesta en los datos dados. No excedas las 3 o 4 oraciones.");
        } else {
            prompt.append("Da una recomendación general de 3 oraciones cortas sobre qué ropa usar y la tendencia proyectada del clima.");
        }
        return prompt.toString();
    }

    private String extractErrorDetail(String body) {
        try {
            JsonObject errJson = JsonParser.parseString(body).getAsJsonObject();
            if (errJson.has("error")) {
                return errJson.getAsJsonObject("error").get("message").getAsString();
            }
        } catch (Exception ignored) {}
        return body;
    }
}
