package weatherapp.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import weatherapp.model.WeatherData;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WeatherService {

    private static final String GEOCODING_API = "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=es";
    private static final String WEATHER_API = "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code&daily=temperature_2m_max,temperature_2m_min";

    public WeatherData getWeatherForCity(String city) throws Exception {
        double[] coords = getCoordinates(city);
        if (coords == null) {
            throw new Exception("Ciudad no encontrada: " + city);
        }
        return fetchWeather(coords[0], coords[1]);
    }

    private double[] getCoordinates(String city) throws Exception {
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.toString());
        String urlString = String.format(GEOCODING_API, encodedCity);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (conn.getResponseCode() != 200) {
            throw new Exception("Error API Geocoding: HTTP " + conn.getResponseCode());
        }

        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            JsonObject jsonObject = jsonElement.getAsJsonObject();

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
        } finally {
            conn.disconnect();
        }
        return null; 
    }

    private WeatherData fetchWeather(double latitude, double longitude) throws Exception {
        String urlString = String.format(java.util.Locale.US, WEATHER_API, latitude, longitude);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (conn.getResponseCode() != 200) {
            throw new Exception("Error API Clima: HTTP " + conn.getResponseCode());
        }

        JsonObject response;
        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            response = JsonParser.parseReader(reader).getAsJsonObject();
        } finally {
            conn.disconnect();
        }
        
        // Clima actual
        JsonObject current = response.getAsJsonObject("current");
        double temp = current.get("temperature_2m").getAsDouble();
        int humidity = current.get("relative_humidity_2m").getAsInt();
        double wind = current.get("wind_speed_10m").getAsDouble();
        int weatherCode = current.get("weather_code").getAsInt();

        // Pronóstico de próximos 7 días
        JsonObject daily = response.getAsJsonObject("daily");
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
