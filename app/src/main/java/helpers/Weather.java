package helpers;

import okhttp3.Request;

public class Weather {
    private final String API_KEY = "3dd8e677b95b2bbd8b523754a15fb51b";
    public Request fetchWeatherData(double latitude, double longitude)  {
        return new Request.Builder()
                .url(String.format("https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&units=metric&appid=%s", latitude, longitude, API_KEY))
                .build();

    }
}
