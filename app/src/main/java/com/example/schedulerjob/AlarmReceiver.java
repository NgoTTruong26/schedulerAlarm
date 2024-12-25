package com.example.schedulerjob;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import helpers.Weather;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class AlarmReceiver extends BroadcastReceiver {

    Weather weather = new Weather();

    private static final String TAG = AlarmReceiver.class.getSimpleName();
    private FusedLocationProviderClient fusedLocationProviderClient;


    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("ALARM_ID", -1);

        long intervalMillis = MainActivity.INTERVALS;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        Log.e(TAG, "Alarm " + alarmId + " - Current Time: " + currentTime);
        Log.e(TAG, "Alarm " + alarmId + " finished");

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        getCurrentLocation(context);

        AlarmSchedule.scheduleExactAlarm(context, alarmId, intervalMillis);
    }

    private void sendWeatherDataToUI(Context context, String weatherDescription, double temperature, String cityName) {
        Intent localIntent = new Intent("WEATHER_DATA_ACTION");
        localIntent.putExtra("weather_description", weatherDescription);
        localIntent.putExtra("temperature", temperature);
        localIntent.putExtra("city_name", cityName);

        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
    }



    private void getCurrentLocation(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Nếu chưa được cấp quyền, thoát ra hoặc yêu cầu quyền
            Log.e(TAG, "Location permission not granted");
            return;
        }

        // Tạo yêu cầu vị trí
        com.google.android.gms.location.LocationRequest locationRequest = com.google.android.gms.location.LocationRequest.create()
                .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(15 * 60 * 1000) // Cập nhật mỗi 15p
                .setFastestInterval(5000); // Tối thiểu 5 giây

        // Đăng ký cập nhật vị trí
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    fetchWeatherData(latitude, longitude, context);
                    Log.d(TAG, "Updated Latitude: " + latitude + ", Longitude: " + longitude);
                } else {
                    Log.d(TAG, "Location is null");
                }
            }
        }, context.getMainLooper());
    }

    public void fetchWeatherData(double latitude, double longitude, Context context) {
        OkHttpClient client = new OkHttpClient();

        // Đảm bảo rằng `weather.fetchWeatherData()` trả về một Request hợp lệ
        Request weatherRequest = weather.fetchWeatherData(latitude, longitude);

        client.newCall(weatherRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject data = new JSONObject(responseBody); // Phân tích cú pháp JSON từ chuỗi
                        Log.d("WeatherAPI", "Response JSON: " + data.toString());

                        // Trích xuất thông tin từ JSON
                        String weatherDescription = data.getJSONArray("weather")
                                .getJSONObject(0)
                                .getString("description");
                        double temperature = data.getJSONObject("main").getDouble("temp");
                        String cityName = data.getString("name");

                        // Log thông tin
                        Log.d("WeatherAPI", "City: " + cityName);
                        Log.d("WeatherAPI", "Temperature: " + temperature + "°C");
                        Log.d("WeatherAPI", "Weather: " + weatherDescription);

                        sendWeatherDataToUI(context, weatherDescription, temperature, cityName);
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "Failed to fetch weather data.");

                }
            }
        });
    }


}
