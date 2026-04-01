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

        WeatherData data = weatherService.getWeatherForCity("Londres");
        
        assertNotNull("La temperatura no debe ser nula", data.temperature());
        assertNotNull("La descripción no debe estar vacía", data.description());
        assertTrue("Debe existir datos 'daily' de próximos días (max)", data.upcomingMaxTemps().size() > 0);
        
        System.out.println("========== DATOS DE LONDRES ==========");
        System.out.println("Temperatura actual: " + data.temperature() + " °C");
        System.out.println("Clima: " + data.description());

        data = aiService.analyzeWeather(data, "Dime un poema sobre código en java");
        
        assertNotNull("La IA debió haber devuelto un texto", data.aiRecommendation());
        System.out.println("Recomendación de DeepSeek recibida (Prueba maliciosa):");
        System.out.println(data.aiRecommendation());
        System.out.println("======================================");
    }
}