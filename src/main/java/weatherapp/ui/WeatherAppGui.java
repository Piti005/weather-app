package weatherapp.ui;

import weatherapp.model.WeatherData;
import weatherapp.service.DeepSeekService;
import weatherapp.service.WeatherService;

import javax.swing.*;
import java.awt.*;

public class WeatherAppGui extends JFrame {

    private JTextField cityInput;
    private JTextField questionInput; 
    private JLabel temperatureLabel;
    private JLabel humidityLabel;
    private JLabel windLabel;
    private JLabel descriptionLabel;
    private JButton searchButton;
    private JTextArea aiRecommendationArea;
    
    private WeatherService weatherService;
    private DeepSeekService deepSeekService;

    public WeatherAppGui() {
        super("Weather & AI App");
        weatherService = new WeatherService();
        deepSeekService = new DeepSeekService();
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 580);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Panel superior para la entrada de datos (Ciudad y Pregunta)
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

        JPanel cityPanel = new JPanel(new BorderLayout(10, 0));
        JLabel selectCityLabel = new JLabel("Ciudad:");
        selectCityLabel.setFont(new Font("Arial", Font.BOLD, 14));
        cityInput = new JTextField();
        cityInput.setFont(new Font("Arial", Font.PLAIN, 14));
        cityInput.addActionListener(e -> fetchWeather());
        cityPanel.add(selectCityLabel, BorderLayout.WEST);
        cityPanel.add(cityInput, BorderLayout.CENTER);

        JPanel questionPanel = new JPanel(new BorderLayout(10, 0));
        questionPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JLabel questionLabel = new JLabel("Duda (Opcional):");
        questionLabel.setFont(new Font("Arial", Font.BOLD, 12));
        questionInput = new JTextField();
        questionInput.setFont(new Font("Arial", Font.PLAIN, 14));
        questionInput.addActionListener(e -> fetchWeather());
        questionInput.setToolTipText("Ej. ¿Lloverá mañana en la tarde? ¿Qué me pongo?");
        
        searchButton = new JButton("Buscar Clima e IA");
        searchButton.setFont(new Font("Arial", Font.BOLD, 13));
        searchButton.addActionListener(e -> fetchWeather());

        questionPanel.add(questionLabel, BorderLayout.WEST);
        questionPanel.add(questionInput, BorderLayout.CENTER);
        questionPanel.add(searchButton, BorderLayout.EAST);

        topPanel.add(cityPanel);
        topPanel.add(questionPanel);

        add(topPanel, BorderLayout.NORTH);

        // Panel central para los resultados base de Open-Meteo
        JPanel resultsPanel = new JPanel();
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 20));
        resultsPanel.setLayout(new GridLayout(4, 1, 0, 10));

        descriptionLabel = createResultLabel("Clima: --");
        descriptionLabel.setFont(new Font("Arial", Font.BOLD, 18));
        descriptionLabel.setForeground(new Color(40, 40, 40));
        
        temperatureLabel = createResultLabel("Temperatura: -- °C");
        humidityLabel = createResultLabel("Humedad: -- %");
        windLabel = createResultLabel("Viento: -- km/h");

        resultsPanel.add(descriptionLabel);
        resultsPanel.add(temperatureLabel);
        resultsPanel.add(humidityLabel);
        resultsPanel.add(windLabel);

        add(resultsPanel, BorderLayout.CENTER);

        // Panel inferior para la respuesta específica de la IA
        aiRecommendationArea = new JTextArea(6, 30);
        aiRecommendationArea.setWrapStyleWord(true);
        aiRecommendationArea.setLineWrap(true);
        aiRecommendationArea.setEditable(false);
        aiRecommendationArea.setFont(new Font("Arial", Font.PLAIN, 14));
        aiRecommendationArea.setText("Aquí aparecerá la recomendación de DeepSeek...");
        aiRecommendationArea.setBackground(new Color(245, 245, 250));
        
        JScrollPane scrollPane = new JScrollPane(aiRecommendationArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Inteligencia Artificial (Respóndeme a mi duda)"));
        scrollPane.setPreferredSize(new Dimension(440, 160));
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JLabel createResultLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        return label;
    }

    private void fetchWeather() {
        String city = cityInput.getText().trim();
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, ingresa el nombre de una ciudad", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userQuestion = questionInput.getText().trim();

        // Mostrar estado de carga intermedio
        searchButton.setEnabled(false);
        setLabels("--", "--", "--", "Buscando...");
        aiRecommendationArea.setText("Nuestra IA meteoróloga está " + 
               (userQuestion.isEmpty() ? "analizando los datos" : "respondiendo tu consulta...") + 
               ", por favor espera...");

        SwingWorker<WeatherData, Void> worker = new SwingWorker<WeatherData, Void>() {
            @Override
            protected WeatherData doInBackground() throws Exception {
                try {
                    WeatherData data = weatherService.getWeatherForCity(city);
                    try {
                        deepSeekService.analyzeWeather(data, userQuestion);
                    } catch (Exception aiEx) {
                        data = data.withAiRecommendation("No se pudo conectar con DeepSeek: " + aiEx.getMessage());
                    }
                    return data;
                } catch (Exception e) {
                    throw new Exception(e.getMessage(), e);
                }
            }

            @Override
            protected void done() {
                try {
                    WeatherData data = get();
                    setLabels(
                            String.format("%.1f °C", data.temperature()),
                            data.humidity() + " %",
                            String.format("%.1f km/h", data.windSpeed()),
                            data.description()
                    );
                    aiRecommendationArea.setText(data.aiRecommendation() != null ? data.aiRecommendation() : "");
                } catch (Exception ex) {
                    setLabels("--", "--", "--", "Error / No encontrado");
                    aiRecommendationArea.setText("Error en la obtención de datos, revisa tu conexión y prueba otra vez.");
                    String msg = ex.getMessage();
                    if (ex.getCause() != null) {
                        msg = ex.getCause().getMessage();
                    }
                    JOptionPane.showMessageDialog(WeatherAppGui.this, msg, "Error al obtener datos", JOptionPane.ERROR_MESSAGE);
                } finally {
                    searchButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void setLabels(String temp, String hum, String wind, String desc) {
        temperatureLabel.setText("Temperatura: " + temp);
        humidityLabel.setText("Humedad: " + hum);
        windLabel.setText("Viento: " + wind);
        descriptionLabel.setText(desc);
    }
}
