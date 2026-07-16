package com.hyeongju.routineapp;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.format.DateFormat;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

// 근무 알람이 실제로 울릴 때 뜨는 화면(2026-07-16, "진짜 기상 알람" 요청으로
// 새로 만듦) — 화면이 꺼져 있거나 잠겨 있어도 이 화면이 전체 화면으로 뜨고
// (ShiftAlarmReceiver가 올린 알림의 fullScreenIntent 덕분), 소리·진동이
// "끄기"를 누르기 전까지 계속 반복됨. 뒤로가기로는 안 닫히게 막아둠 —
// 사용자가 버튼을 눌러야만 꺼지는 게 목적(요청 그대로).
public class ShiftAlarmActivity extends Activity {
    private static final long SNOOZE_MINUTES = 5;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private int alarmId;
    private String shiftName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 화면이 꺼져 있거나 잠겨 있어도 이 화면이 그 위로 뜨게 함 — 전화
        // 수신 화면·진짜 알람시계 앱들이 쓰는 것과 같은 방식. API 27(O_MR1)
        // 부터는 이 방식(런타임 API)을 쓰고, 그보다 낮으면 예전 방식(윈도우
        // 플래그)을 씀 — 매니페스트에 선언하는 속성이 아니라 코드에서 해야
        // 함(그런 매니페스트 속성 자체가 없음, 혼동 주의).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_shift_alarm);

        alarmId = getIntent().getIntExtra(ShiftAlarmReceiver.EXTRA_ALARM_ID, 0);
        shiftName = getIntent().getStringExtra(ShiftAlarmReceiver.EXTRA_SHIFT_NAME);
        if (shiftName == null) shiftName = "";

        TextView titleView = findViewById(R.id.alarm_shift_name);
        titleView.setText(shiftName.isEmpty() ? "근무 알람" : (shiftName + " 근무일이에요"));

        TextView timeView = findViewById(R.id.alarm_time);
        timeView.setText(DateFormat.format("HH:mm", Calendar.getInstance()));

        findViewById(R.id.alarm_stop_button).setOnClickListener(v -> stopAlarm());
        findViewById(R.id.alarm_snooze_button).setOnClickListener(v -> snoozeAlarm());

        startRinging();
    }

    private void startRinging() {
        // 소리·진동 전체 켜기/끄기(2026-07-16 추가) — 앱 설정 화면에서 고른
        // 값을 그대로 읽음. 예약해둔 시점이 아니라 "알람이 실제로 울리는 이
        // 순간"의 값을 읽는 것이라, 켰다 껐다 한 뒤 아직 안 울린 알람도 항상
        // 최신 설정을 따름.
        SharedPreferences prefs = getSharedPreferences(ShiftAlarmPlugin.PREFS_NAME, MODE_PRIVATE);
        boolean soundEnabled = prefs.getBoolean(ShiftAlarmPlugin.KEY_SOUND_ENABLED, true);
        boolean vibrateEnabled = prefs.getBoolean(ShiftAlarmPlugin.KEY_VIBRATE_ENABLED, true);

        // 소리는 계속 알람 전용 소리 스트림(AudioAttributes.USAGE_ALARM)으로
        // 재생함 — 기기가 무음/진동 모드여도 안드로이드가 이 스트림은 따로
        // 취급해서 그대로 남(이 앱 설정이 켜져 있으면 기기의 무음/진동 모드와
        // 무관하게 울리게 해달라는 요청과 정확히 맞음). 이 앱 설정에서
        // 소리를 꺼뒀을 때만 아예 안 틀도록 여기서 막음.
        if (soundEnabled) {
            Uri alarmSound = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
                mediaPlayer.setDataSource(this, alarmSound);
                mediaPlayer.setLooping(true);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (Exception e) {
                // 소리를 못 틀어도 진동은(켜져 있다면) 계속 울리니 알람 자체는 동작함
            }
        }

        if (vibrateEnabled) {
            long[] pattern = {0, 800, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vm != null ? vm.getDefaultVibrator() : null;
            } else {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            }
            // Vibrator.vibrate()를 직접 호출하는 건 알림 채널을 거치는 진동과
            // 달리 기기의 무음/진동 모드·방해금지 설정과 무관하게 동작함(이
            // 앱 설정이 우선한다는 요청과 맞음) — 진동 자체를 시스템에서
            // 완전히 꺼둔 극히 일부 기기·설정에서만 예외적으로 안 울릴 수 있음.
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        }
    }

    private void stopRinging() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                // 무시
            }
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        NotificationManagerCompat.from(this).cancel(alarmId);
    }

    private void stopAlarm() {
        stopRinging();
        finish();
    }

    private void snoozeAlarm() {
        stopRinging();
        long snoozeAt = System.currentTimeMillis() + SNOOZE_MINUTES * 60 * 1000;
        ShiftAlarmScheduler.scheduleOne(this, alarmId, snoozeAt, shiftName);
        finish();
    }

    // 뒤로가기로는 안 닫히게 함 — 꼭 "끄기"나 "다시 울리기" 버튼을 눌러야
    // 알람이 멈춤(사용자가 명시적으로 요청한 "꺼야 꺼지는 알람" 동작).
    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRinging();
    }
}
