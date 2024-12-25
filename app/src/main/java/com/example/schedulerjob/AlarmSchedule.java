package com.example.schedulerjob;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class AlarmSchedule {
    private static final String TAG = AlarmSchedule.class.getSimpleName();

    public static void scheduleExactAlarm(Context context, int alarmId, long intervalMillis) {
        // Kiểm tra phiên bản Android, từ Android 12 trở lên cần kiểm tra quyền
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                // Nếu có quyền, lên lịch báo thức
                setExactAlarm(context, alarmManager, alarmId, intervalMillis);
            } else {
                // Nếu không có quyền, yêu cầu cấp quyền
                Intent permissionIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                context.startActivity(permissionIntent);
            }
        } else {
            // Với các phiên bản Android thấp hơn, không cần kiểm tra quyền
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                setExactAlarm(context, alarmManager, alarmId, intervalMillis);
            }
        }
    }

    // Hàm phụ trợ để thực sự lên lịch báo thức
    private static void setExactAlarm(Context context, AlarmManager alarmManager, int alarmId, long intervalMillis) {
        // Tạo Intent cho BroadcastReceiver
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("ALARM_ID", alarmId);

        // PendingIntent để quản lý Broadcast
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        // Tính toán thời điểm báo thức
        long triggerAtMillis = System.currentTimeMillis() + intervalMillis;

        // Lên lịch báo thức
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);

        Toast.makeText(context, "Alarm " + alarmId + " scheduled successfully", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Alarm " + alarmId + " scheduled successfully");
    }
}

