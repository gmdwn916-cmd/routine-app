package com.hyeongju.routineapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebSettings;

import com.getcapacitor.BridgeActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends BridgeActivity {
    // 위젯(2/3/4/5)을 탭했을 때 "어느 화면으로 바로 들어가야 하는지"를 담아서
    // MainActivity를 여는 인텐트에 실어 보낸 값 — WidgetBridgePlugin의
    // getPendingNavTarget()/clearPendingNavTarget()이 이 키로 SharedPreferences에서
    // 읽고 지움. 웹뷰가 로딩 중일 수도 있는 onCreate 시점에 바로 JS를 실행하는
    // 대신(타이밍이 불안정함), 위젯 1의 "임시 우편함"과 같은 방식으로 SharedPreferences에
    // 저장해두고 JS쪽(syncWidgetNavTarget)이 앱 시작/포그라운드 복귀 시 읽어가게 함.
    static final String EXTRA_WIDGET_NAV = "widget_nav";
    static final String EXTRA_WIDGET_NAV_MONTH = "widget_nav_month";

    private static final String ALARM_CHANNEL_ID = "shift-alarm";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(WidgetBridgePlugin.class);
        super.onCreate(savedInstanceState);
        // 개발 중 계속 새 APK를 설치해서 테스트하는데, 안드로이드는 APK를 새로
        // 설치해도 앱 데이터/캐시는 그대로 유지해서 웹뷰가 예전 index.html을
        // 계속 캐시해서 보여주는 문제가 있었음(위젯 데이터 형식이 계속 옛날
        // 방식으로 오던 원인이 이것). 캐시를 아예 안 쓰게 해서 항상 방금 설치한
        // 최신 코드로 실행되게 함.
        this.bridge.getWebView().getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        ensureAlarmChannel();
        savePendingNavTarget(getIntent());
    }

    // 근무유형별 알람이 무음/진동 모드에서도 항상 소리로 울리게 함(2026-07-14).
    // @capacitor/local-notifications 플러그인의 createChannel()은 채널의 소리를
    // 항상 AudioAttributes.USAGE_NOTIFICATION으로 고정해서 만듦(플러그인 자체
    // 네이티브 코드에 하드코딩돼 있어 JS에서 바꿀 수 없음) — 이 용도는 무음/
    // 진동 모드일 때 안드로이드가 그대로 존중해서 안 울리거나 진동만 됨. 진짜
    // 알람시계 앱들이 쓰는 것과 같은 방식(USAGE_ALARM + 알람용 소리 스트림)으로
    // 직접 채널을 만들어야만 무음/진동 모드를 무시하고 항상 소리가 남 — 그래서
    // 이 채널만은 플러그인을 거치지 않고 안드로이드 API로 직접 만듦. 채널
    // 속성(소리/중요도 등)은 한 번 만들면 안드로이드가 이후 바뀐 값을 무시하므로,
    // 예전(이 수정 이전) 버전에서 이미 잘못된 설정으로 만들어져 남아있을 수 있는
    // 채널을 먼저 지우고 새로 만듦 — 앱을 켤 때마다 실행되지만 가벼운 작업이라
    // 문제없음. index.html 쪽의 LN.createChannel() 호출은 지웠음(같은 id로
    // 또 만들려고 해도 이미 존재해서 무시되고, 남겨두면 "어느 쪽이 진짜 설정을
    // 하는지" 헷갈리므로) — 다시 JS에서 이 채널을 만들려고 하지 말 것.
    private void ensureAlarmChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;

        nm.deleteNotificationChannel(ALARM_CHANNEL_ID);

        NotificationChannel channel = new NotificationChannel(
            ALARM_CHANNEL_ID, "근무 알람", NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("근무유형별로 설정한 알람");
        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        channel.enableVibration(true);

        AudioAttributes attrs = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build();
        Uri alarmSound = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        channel.setSound(alarmSound, attrs);

        nm.createNotificationChannel(channel);
    }

    // launchMode="singleTask"라 앱이 이미 떠 있을 때 위젯을 또 누르면 onCreate가
    // 아니라 여기로 옴 — 그래서 여기서도 똑같이 저장해줘야 함.
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        savePendingNavTarget(intent);
    }

    private void savePendingNavTarget(Intent intent) {
        if (intent == null) return;
        String target = intent.getStringExtra(EXTRA_WIDGET_NAV);
        if (target == null) return;

        try {
            JSONObject obj = new JSONObject();
            obj.put("target", target);
            String month = intent.getStringExtra(EXTRA_WIDGET_NAV_MONTH);
            if (month != null) obj.put("month", month);

            SharedPreferences prefs = getSharedPreferences(WidgetBridgePlugin.PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(WidgetBridgePlugin.KEY_PENDING_NAV_TARGET, obj.toString()).apply();
        } catch (JSONException e) {
            // 무시 — 이번 한 번은 원래 화면으로 열리고 다음 위젯 탭부터 정상 동작
        }

        // 같은 인텐트를 나중에 또 처리하지 않도록 extra를 비워둠(예: 화면 회전으로
        // Activity가 재생성될 때 getIntent()가 같은 값을 또 들고 있는 것 방지).
        intent.removeExtra(EXTRA_WIDGET_NAV);
        intent.removeExtra(EXTRA_WIDGET_NAV_MONTH);
    }
}
