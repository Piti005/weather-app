package weatherapp.model;

import java.util.List;

public record WeatherData(
    double temperature,
    int humidity,
    double windSpeed,
    String description,
    List<Double> upcomingMaxTemps,
    List<Double> upcomingMinTemps,
    String aiRecommendation
) {
    public WeatherData(double temperature, int humidity, double windSpeed, String description, 
                       List<Double> upcomingMaxTemps, List<Double> upcomingMinTemps) {
        this(temperature, humidity, windSpeed, description, upcomingMaxTemps, upcomingMinTemps, null);
    }

    public WeatherData withAiRecommendation(String recommendation) {
        return new WeatherData(temperature, humidity, windSpeed, description, 
                               upcomingMaxTemps, upcomingMinTemps, recommendation);
    }

    @Override
    public String toString() {
        return "WeatherData{temp=" + temperature + ", desc='" + description + "'}";
    }
}