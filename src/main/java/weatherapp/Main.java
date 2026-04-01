package weatherapp;

import weatherapp.model.WeatherData;
import weatherapp.service.DeepSeekService;
import weatherapp.service.WeatherService;
import weatherapp.ui.WeatherAppGui;

import java.awt.GraphicsEnvironment;
import java.util.Scanner;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Validación inteligente para evitar que la aplicación 'crashee' en servidores de texto
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("=========================================================");
            System.out.println("🖥️ No se detectó interfaz gráfica. Iniciando MODO CONSOLA");
            System.out.println("=========================================================");
            runConsoleMode();
        } else {
            // Modo gráfico tradicional
            SwingUtilities.invokeLater(() -> {
                WeatherAppGui gui = new WeatherAppGui();
                gui.setVisible(true);
            });
        }
    }

    private static void runConsoleMode() {
        WeatherService weatherService = new WeatherService();
        DeepSeekService deepSeekService = new DeepSeekService();
        
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("🏙️ ¿De qué ciudad quieres saber el clima y recibir consejo? ");
            String city = scanner.nextLine();

            if (city == null || city.trim().isEmpty()) {
                System.out.println("Ciudad no válida. Saliendo...");
                return;
            }

            System.out.print("❓ (Opcional) ¿Tienes alguna pregunta específica sobre este clima?: ");
            String userQuestion = scanner.nextLine().trim();

                System.out.println("⏳ Obteniendo datos de Open-Meteo...");
                try {
                    WeatherData data = weatherService.getWeatherForCity(city);
                    System.out.println("\n🌡️ Temperatura: " + data.temperature() + " °C");
                    System.out.println("💧 Humedad:    " + data.humidity() + " %");
                    System.out.println("💨 Viento:     " + data.windSpeed() + " km/h");
                    System.out.println("☁️ Resumen:    " + data.description());

                    System.out.println("\n🧠 La IA de DeepSeek está analyzing...");
                    data = deepSeekService.analyzeWeather(data, userQuestion);
                    
                    System.out.println("---------------------------------------------------------");
                    System.out.println("🤖 RECOMENDACIÓN DE LA IA:");
                    System.out.println(data.aiRecommendation());
                    System.out.println("---------------------------------------------------------");

            } catch (Exception e) {
                System.out.println("❌ Ocurrió un error al obtener la información: " + e.getMessage());
            }
        }
    }
}
