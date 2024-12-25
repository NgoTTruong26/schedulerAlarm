package com.example.schedulerjob;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import helpers.Weather;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    private static final String TAG = MainActivity.class.getSimpleName();
    protected static final int ALARM_IDS = 101;
    protected static final long INTERVALS =15 * 60 * 1000; // 15, 20, 25 minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button startButtonAlarm = findViewById(R.id.startSchedulerAlarm);
        Button stopButtonAlarm = findViewById(R.id.stopSchedulerAlarm);


        startButtonAlarm.setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Nếu quyền chưa được cấp, yêu cầu quyền
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_LOCATION_PERMISSION
                );
            } else {
                onClickStartSchedulerAlarm(ALARM_IDS, INTERVALS);
            }

        });

        stopButtonAlarm.setOnClickListener(view -> {
            onClickCancelSchedulerAlarm(ALARM_IDS);
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(weatherDataReceiver,
                new IntentFilter("WEATHER_DATA_ACTION"));

    }

    private final BroadcastReceiver weatherDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String weatherDescription = intent.getStringExtra("weather_description");
            double temperature = intent.getDoubleExtra("temperature", 0.0);
            String cityName = intent.getStringExtra("city_name");

            // Cập nhật UI
            updateWeatherUI(weatherDescription, temperature, cityName);
        }
    };

    private void updateWeatherUI(String weatherDescription, double temperature, String cityName) {
        TextView weatherInfoTextView = findViewById(R.id.weatherInfoTextView);

        String weatherInfo = "City: " + cityName + "\n"
                + "Temperature: " + temperature + "°C\n"
                + "Weather: " + weatherDescription;

        weatherInfoTextView.setText(weatherInfo);
    }

    private void triggerAlarmAction(int alarmId) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("ALARM_ID", alarmId);
        sendBroadcast(intent); // Gọi trực tiếp BroadcastReceiver
    }
    private void onClickStartSchedulerAlarm(int alarmId, long intervalMillis) {

        triggerAlarmAction(alarmId);

        AlarmSchedule.scheduleExactAlarm(this, alarmId, intervalMillis);
    }

    private void onClickCancelSchedulerAlarm(int alarmId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Toast.makeText(this, "Alarm " + alarmId + " cancelled", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Alarm " + alarmId + " cancelled");
        }
    }



}