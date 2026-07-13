package com.hyeongju.routineapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

// 이번 달 달력을 색으로 보여주는 읽기 전용 위젯. 근무 계산은 전혀 모름 —
// 웹(JS)이 이미 계산해서 WidgetBridgePlugin.setMonthCalendarData()로 넘겨준
// 데이터(SharedPreferences에 JSON으로 저장됨)를 그대로 그리기만 함. 요일 순서
// (월~일/일~월)와 각 칸이 그 달에 속하는지도 전부 JS가 이미 정리해서 넘겨줌 —
// 여기서 새로 계산하는 건 "오늘" 강조뿐(오늘 날짜 문자열과 각 칸의 날짜 문자열을
// 비교하는 단순 비교라 근무 로직이 아님 — 중복 구현 문제 없음).
public class MonthCalendarWidgetProvider extends AppWidgetProvider {
    public static final String PREFS_NAME = "widget_bridge";
    public static final String KEY_MONTH_DATA = "month_calendar_data";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateOne(context, appWidgetManager, id);
        }
    }

    // WidgetBridgePlugin이 새 데이터를 받았을 때 즉시 다시 그리기 위해 호출.
    public static void refreshAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, MonthCalendarWidgetProvider.class));
        for (int id : ids) {
            updateOne(context, mgr, id);
        }
    }

    private static int idFor(Context context, String name) {
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }

    private static void updateOne(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_month_calendar);

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(idFor(context, "widget_month_root"), openPending);

        // 자료가 없거나 깨져 있어도 안전하게 빈 칸으로 시작
        for (int i = 0; i < 7; i++) {
            views.setTextViewText(idFor(context, "header_" + i), "");
        }
        for (int i = 0; i < 42; i++) {
            int cid = idFor(context, "cell_" + i);
            views.setTextViewText(cid, "");
            views.setInt(cid, "setBackgroundColor", 0x00000000);
            views.setTextColor(cid, 0xFF1C1C1E);
        }
        views.setTextViewText(idFor(context, "widget_month_label"), "");

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_MONTH_DATA, null);

        if (raw != null) {
            try {
                JSONObject obj = new JSONObject(raw);
                views.setTextViewText(idFor(context, "widget_month_label"), obj.optString("monthLabel", ""));

                JSONArray headers = obj.optJSONArray("headers");
                if (headers != null) {
                    for (int i = 0; i < headers.length() && i < 7; i++) {
                        views.setTextViewText(idFor(context, "header_" + i), headers.optString(i, ""));
                    }
                }

                String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());

                JSONArray days = obj.optJSONArray("days");
                if (days != null) {
                    for (int i = 0; i < days.length() && i < 42; i++) {
                        Object dayObj = days.opt(i);
                        if (!(dayObj instanceof JSONObject)) continue; // 그 달에 속하지 않는 빈 칸
                        JSONObject day = (JSONObject) dayObj;
                        String dateStr = day.optString("date", "");
                        int dayNum = day.optInt("dayNum", 0);
                        String color = day.optString("color", "");
                        int cid = idFor(context, "cell_" + i);

                        views.setTextViewText(cid, String.valueOf(dayNum));
                        if (!color.isEmpty()) {
                            try {
                                int base = Color.parseColor(color);
                                int tinted = (base & 0x00FFFFFF) | 0x33000000; // 약 20% 불투명도
                                views.setInt(cid, "setBackgroundColor", tinted);
                            } catch (IllegalArgumentException e) {
                                // 색상 파싱 실패 시 배경 없이 숫자만 표시
                            }
                        }
                        if (dateStr.equals(todayStr)) {
                            views.setTextColor(cid, 0xFF007AFF);
                        }
                    }
                }
            } catch (Exception e) {
                // 저장된 데이터가 깨져 있으면 위에서 이미 비워둔 상태로 둠
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
