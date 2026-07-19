package com.hyeongju.routineapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    // "day" 타겟(2026-07-19 추가, 스케줄 위젯 날짜 팝업에서 앱 열기)용 —
    // 위 두 extra와 같은 방식으로 그 날짜 하나만 실어 보냄.
    static final String EXTRA_WIDGET_NAV_DATE = "widget_nav_date";

    private static final String ALARM_CHANNEL_ID = "shift-alarm";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(WidgetBridgePlugin.class);
        // 알람 기능(2026-07-16 추가) 관련 초기화가 뭔가 잘못돼도 앱 자체는
        // 반드시 열려야 하므로, 이 부분만 방어적으로 try-catch로 감쌈 — 새
        // 기능 하나의 문제로 앱 전체가 안 열리는 사고를 막기 위함.
        try {
            registerPlugin(ShiftAlarmPlugin.class);
        } catch (Throwable t) {
            // 무시 — 알람 기능만 못 쓰게 될 뿐, 앱은 정상적으로 열림
        }
        super.onCreate(savedInstanceState);
        // 개발 중 계속 새 APK를 설치해서 테스트하는데, 안드로이드는 APK를 새로
        // 설치해도 앱 데이터/캐시는 그대로 유지해서 웹뷰가 예전 index.html을
        // 계속 캐시해서 보여주는 문제가 있었음(위젯 데이터 형식이 계속 옛날
        // 방식으로 오던 원인이 이것). 캐시를 아예 안 쓰게 해서 항상 방금 설치한
        // 최신 코드로 실행되게 함.
        this.bridge.getWebView().getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        try {
            ensureAlarmChannel();
        } catch (Throwable t) {
            // 무시 — 알림 채널 생성 실패는 알람 기능에만 영향, 앱은 정상적으로 열림
        }
        savePendingNavTarget(getIntent());
    }

    // 근무유형별 알람 채널(2026-07-16, "진짜 기상 알람"으로 재구현하며 정리) —
    // 이제 소리·진동은 전부 ShiftAlarmActivity(알람이 울릴 때 뜨는 화면)가
    // 직접 반복해서 재생함(꺼야 꺼지는 알람이 목적이라 한 번만 울리고 마는
    // 채널 소리로는 부족함) — 그래서 이 채널 자체는 소리·진동 없이 조용하게
    // 만듦(채널 소리·알림 진동까지 같이 울리면 화면이 뜨기 전 아주 잠깐
    // 겹쳐 울리는 어색함이 생길 수 있어서). 채널은 fullScreenIntent가 있는
    // 알림을 올리기 위한 최소한의 통로 역할만 함 — importance는 여전히
    // HIGH로 둬야 fullScreenIntent가 제대로 동작함. 채널 속성은 한 번 만들면
    // 안드로이드가 이후 바뀐 값을 무시하므로, 예전(알림 소리가 있던) 버전이
    // 만들어둔 채널이 남아있을 수 있어 항상 먼저 지우고 새로 만듦.
    private void ensureAlarmChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;

        nm.deleteNotificationChannel(ALARM_CHANNEL_ID);

        NotificationChannel channel = new NotificationChannel(
            ALARM_CHANNEL_ID, "근무 알람", NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("근무유형별로 설정한 알람 — 화면 전체에 뜨는 알람 화면으로 울림");
        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        channel.enableVibration(false);
        channel.setSound(null, null);

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
            String date = intent.getStringExtra(EXTRA_WIDGET_NAV_DATE);
            if (date != null) obj.put("date", date);

            SharedPreferences prefs = getSharedPreferences(WidgetBridgePlugin.PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(WidgetBridgePlugin.KEY_PENDING_NAV_TARGET, obj.toString()).apply();
        } catch (JSONException e) {
            // 무시 — 이번 한 번은 원래 화면으로 열리고 다음 위젯 탭부터 정상 동작
        }

        // 같은 인텐트를 나중에 또 처리하지 않도록 extra를 비워둠(예: 화면 회전으로
        // Activity가 재생성될 때 getIntent()가 같은 값을 또 들고 있는 것 방지).
        intent.removeExtra(EXTRA_WIDGET_NAV);
        intent.removeExtra(EXTRA_WIDGET_NAV_MONTH);
        intent.removeExtra(EXTRA_WIDGET_NAV_DATE);
    }
}
