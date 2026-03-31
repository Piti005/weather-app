package weatherapp.model;

import java.util.List;

public class WeatherData {
    private double temperature;
    private int humidity;
    private double windSpeed;
    private String description;
    
    // Pronósticos de 3 a 7 días
    private List<Double> upcomingMaxTemps;
    private List<Double> upcomingMinTemps;
    
    // Recomendación de la IA
    private String aiRecommendation;

    public WeatherData(double temperature, int humidity, double windSpeed, String description, 
                       List<Double> upcomingMaxTemps, List<Double> upcomingMinTemps) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.description = description;
        this.upcomingMaxTemps = upcomingMaxTemps;
        this.upcomingMinTemps = upcomingMinTemps;
    }

    public double getTemperature() { return temperature; }
    public int getHumidity() { return humidity; }
    public double getWindSpeed() { return windSpeed; }
    public String getDescription() { return description; }
    public List<Double> getUpcomingMaxTemps() { return upcomingMaxTemps; }
    public List<Double> getUpcomingMinTemps() { return upcomingMinTemps; }
    
    public String getAiRecommendation() { return aiRecommendation; }
    public void setAiRecommendation(String aiRecommendation) { this.aiRecommendation = aiRecommendation; }

    @Override
    public String toString() {
        return "WeatherData{" +
                "temperature=" + temperature +
                ", min/max deps=" + upcomingMinTemps.size() +
                ", ai='" + (aiRecommendation != null ? aiRecommendation.substring(0, Math.min(20, aiRecommendation.length())) + "..." : "null") + '\'' +
                '}';
    }
}
