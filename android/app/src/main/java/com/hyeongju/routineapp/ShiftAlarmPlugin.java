package com.hyeongju.routineapp;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// 근무유형별 알람을 실제로 안드로이드에 예약/취소하는 다리. JS(scheduleShiftAlarms())가
// "이 날짜들에 이 근무 이름으로 알람을 걸어야 한다"를 계산해서 이 플러그인에
// 넘기면, 여기서 ShiftAlarmScheduler를 통해 AlarmManager.setAlarmClock()으로
// 진짜 예약함. 예전엔 @capacitor/local-notifications로 짧게 뜨는 알림만
// 만들었는데, "화면을 다 덮고 꺼야 꺼지는 진짜 기상 알람"을 만들려면 그
// 플러그인으로는 안 돼서(전체 화면 강제 띄우기·소리 반복이 안 됨) 이 전용
// 플러그인을 새로 만듦(2026-07-16).
@CapacitorPlugin(name = "ShiftAlarm")
public class ShiftAlarmPlugin extends Plugin {
    static final String PREFS_NAME = "widget_bridge";
    private static final String KEY_SCHEDULED_IDS = "shift_alarm_scheduled_ids";
    // 소리·진동 전체 켜기/끄기(2026-07-16 추가) — ShiftAlarmActivity가 알람이
    // 실제로 울리는 그 순간에 이 값을 읽어서 반영함(예약해둔 시점이 아니라
    // 울리는 시점 기준 — 켰다 껐다 한 뒤 아직 안 울린 알람도 항상 최신
    // 설정을 따르게 하려는 것). 기본값(prefs에 아직 없을 때)은 둘 다 켜짐.
    static final String KEY_SOUND_ENABLED = "shift_alarm_sound_enabled";
    static final String KEY_VIBRATE_ENABLED = "shift_alarm_vibrate_enabled";

    // items: [{id, epochMillis, shiftName}, ...] — 항상 기존에 예약해둔 것부터
    // 전부 취소하고 새로 깖(JS쪽이 그때그때 "지금 시점에서 앞으로 14일치가
    // 이렇다"를 통째로 다시 계산해서 넘기는 방식이라, 예약 쪽도 매번 통째로
    // 새로 까는 게 더 확실함 — 예전 LocalNotifications 방식과 같은 원칙).
    @PluginMethod
    public void scheduleAll(PluginCall call) {
        JSArray items = call.getArray("items");
        if (items == null) {
            call.reject("items missing");
            return;
        }
        boolean sound = call.getBoolean("sound", true);
        boolean vibrate = call.getBoolean("vibrate", true);

        cancelAllInternal();

        JSONArray newIds = new JSONArray();
        try {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                int id = item.getInt("id");
                long epochMillis = item.getLong("epochMillis");
                String shiftName = item.optString("shiftName", "");
                ShiftAlarmScheduler.scheduleOne(getContext(), id, epochMillis, shiftName);
                newIds.put(id);
            }
        } catch (JSONException e) {
            call.reject("invalid items", e);
            return;
        }

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putString(KEY_SCHEDULED_IDS, newIds.toString())
            .putBoolean(KEY_SOUND_ENABLED, sound)
            .putBoolean(KEY_VIBRATE_ENABLED, vibrate)
            .apply();
        call.resolve();
    }

    @PluginMethod
    public void cancelAll(PluginCall call) {
        cancelAllInternal();
        call.resolve();
    }

    // 안드로이드 14(API 34)부터는 "화면 전체 강제로 띄우기" 권한(USE_FULL_SCREEN_
    // INTENT, 매니페스트에 선언은 해뒀음)을 앱 설치만으로 자동으로는 안 주고,
    // 사용자가 설정 화면에서 직접 허락해야 함(이전 버전까지는 자동으로 허락됨) —
    // 안 그러면 알람 시각에 화면이 안 뜨고 그냥 조용한 알림 하나만 뜸. 이미
    // 허락돼 있으면(또는 API 34 미만이라 해당 없으면) 아무 것도 안 하고, 안
    // 돼있으면 그 설정 화면으로 바로 안내함.
    @PluginMethod
    public void ensureFullScreenIntentAllowed(PluginCall call) {
        JSObject ret = new JSObject();
        boolean allowed = true;
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager nm = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            allowed = nm != null && nm.canUseFullScreenIntent();
            if (!allowed) {
                try {
                    Intent intent = new Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT");
                    intent.setData(Uri.parse("package:" + getContext().getPackageName()));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(intent);
                } catch (Exception e) {
                    // 이 설정 화면이 없는 기기면 조용히 무시 — 앱은 계속 정상 동작,
                    // 다만 그 기기에서는 알람이 전체 화면으로 안 뜰 수 있음
                }
            }
        }
        ret.put("allowed", allowed);
        call.resolve(ret);
    }

    private void cancelAllInternal() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_SCHEDULED_IDS, "[]");
        try {
            JSONArray ids = new JSONArray(raw);
            for (int i = 0; i < ids.length(); i++) {
                ShiftAlarmScheduler.cancelOne(getContext(), ids.getInt(i));
            }
        } catch (JSONException e) {
            // 무시 — 저장된 목록이 깨져 있으면 취소할 것도 알 수 없음
        }
        prefs.edit().putString(KEY_SCHEDULED_IDS, "[]").apply();
    }
}
