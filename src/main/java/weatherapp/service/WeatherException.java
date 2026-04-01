package weatherapp.service;

public class WeatherException extends Exception {
    
    public WeatherException(String message) {
        super(message);
    }

    public WeatherException(String message, Throwable cause) {
        super(message, cause);
    }

    public static WeatherException cityNotFound(String city) {
        return new WeatherException("Ciudad no encontrada: " + city);
    }

    public static WeatherException apiError(String apiName, int statusCode) {
        return new WeatherException("Error en " + apiName + ": HTTP " + statusCode);
    }

    public static WeatherException networkError(Throwable cause) {
        return new WeatherException("Error de conexión: " + cause.getMessage(), cause);
    }
}