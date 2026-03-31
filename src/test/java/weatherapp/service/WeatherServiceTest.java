package weatherapp.service;

import org.junit.Test;
import weatherapp.model.WeatherData;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WeatherServiceTest {

    @Test
    public void testWeatherServiceAndDeepSeek() throws Exception {
        WeatherService weatherService = new WeatherService();
        DeepSeekService aiService = new DeepSeekService();

        // 1. Probar que recibe coordenadas y datos de clima (Londres)
        WeatherData data = weatherService.getWeatherForCity("Londres");
        
        assertNotNull("La temperatura no debe ser nula", data.getTemperature());
        assertNotNull("La descripción no debe estar vacía", data.getDescription());
        assertTrue("Debe existir datos 'daily' de próximos días (max)", data.getUpcomingMaxTemps().size() > 0);
        
        System.out.println("========== DATOS DE LONDRES ==========");
        System.out.println("Temperatura actual: " + data.getTemperature() + " °C");
        System.out.println("Clima: " + data.getDescription());

        // 2. Probar que la IA puede interpretar esto con una pregunta restrictiva
        aiService.analyzeWeather(data, "Dime un poema sobre código en java");
        
        assertNotNull("La IA debió haber devuelto un texto", data.getAiRecommendation());
        System.out.println("Recomendación de DeepSeek recibida (Prueba maliciosa):");
        System.out.println(data.getAiRecommendation());
        System.out.println("======================================");
    }
}
