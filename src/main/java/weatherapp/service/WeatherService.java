package weatherapp.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import weatherapp.model.WeatherData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class WeatherService {

    private static final String GEOCODING_API = "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=es";
    private static final String WEATHER_API = "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code&daily=temperature_2m_max,temperature_2m_min";
    private static final int TIMEOUT_SECONDS = 10;

    private final HttpClient httpClient;

    public WeatherService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    public WeatherData getWeatherForCity(String city) throws WeatherException {
        if (city == null || city.trim().isEmpty()) {
            throw WeatherException.cityNotFound(city != null ? city : "null");
        }
        
        double[] coords = getCoordinates(city.trim());
        if (coords == null) {
            throw WeatherException.cityNotFound(city);
        }
        return fetchWeather(coords[0], coords[1]);
    }

    private double[] getCoordinates(String city) throws WeatherException {
        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String urlString = String.format(GEOCODING_API, encodedCity);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw WeatherException.apiError("Geocoding", response.statusCode());
            }

            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();

            if (jsonObject.has("results")) {
                JsonArray results = jsonObject.getAsJsonArray("results");
                if (results.size() > 0) {
                    JsonObject firstResult = results.get(0).getAsJsonObject();
                    return new double[]{
                            firstResult.get("latitude").getAsDouble(),
                            firstResult.get("longitude").getAsDouble()
                    };
                }
            }
            return null;
            
        } catch (WeatherException e) {
            throw e;
        } catch (Exception e) {
            throw WeatherException.networkError(e);
        }
    }

    private WeatherData fetchWeather(double latitude, double longitude) throws WeatherException {
        try {
            String urlString = String.format(java.util.Locale.US, WEATHER_API, latitude, longitude);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw WeatherException.apiError("Clima", response.statusCode());
            }

            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

            JsonObject current = responseJson.getAsJsonObject("current");
            double temp = current.get("temperature_2m").getAsDouble();
            int humidity = current.get("relative_humidity_2m").getAsInt();
            double wind = current.get("wind_speed_10m").getAsDouble();
            int weatherCode = current.get("weather_code").getAsInt();

            JsonObject daily = responseJson.getAsJsonObject("daily");
            JsonArray maxTempsArray = daily.getAsJsonArray("temperature_2m_max");
            JsonArray minTempsArray = daily.getAsJsonArray("temperature_2m_min");

            List<Double> maxTemps = new ArrayList<>();
            List<Double> minTemps = new ArrayList<>();
            
            for (int i = 0; i < Math.min(maxTempsArray.size(), 5); i++) {
                maxTemps.add(maxTempsArray.get(i).getAsDouble());
                minTemps.add(minTempsArray.get(i).getAsDouble());
            }

            String description = getWeatherDescription(weatherCode);

            return new WeatherData(temp, humidity, wind, description, maxTemps, minTemps);

        } catch (WeatherException e) {
            throw e;
        } catch (Exception e) {
            throw WeatherException.networkError(e);
        }
    }

    private String getWeatherDescription(int code) {
        if (code == 0) return "Cielo Despejado";
        if (code >= 1 && code <= 3) return "Parcialmente Nublado";
        if (code >= 45 && code <= 48) return "Niebla";
        if (code >= 51 && code <= 57) return "Llovizna";
        if (code >= 61 && code <= 67) return "Lluvia";
        if (code >= 71 && code <= 77) return "Nieve";
        if (code >= 80 && code <= 82) return "Lluvia Fuerte";
        if (code >= 95 && code <= 99) return "Tormenta Electrica";
        return "Desconocido (" + code + ")";
    }
}