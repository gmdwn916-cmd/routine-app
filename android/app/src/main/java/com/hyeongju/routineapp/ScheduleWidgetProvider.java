package com.hyeongju.routineapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

// 2주씩 묶은 스케줄을 요일 그리드(2줄×7칸)로 보여주는 읽기 전용 위젯 + 페이지
// 넘기기. 위젯 2(달력)와 완전히 같은 원칙 — 근무 계산·할 일 목록 판단은 전부
// JS(index.html)가 끝내고 결과만 넘겨줌(WidgetBridgePlugin.setScheduleData).
// 여기서 하는 일은 그 결과를 그리는 것과 페이지 위치 기억뿐 — 근무·할일 로직 없음.
public class ScheduleWidgetProvider extends AppWidgetProvider {
    public static final String PREFS_NAME = "widget_bridge";
    public static final String KEY_SCHEDULE_DATA = "schedule_widget_data";
    public static final String KEY_PAGE_INDEX = "schedule_widget_page_index";

    private static final int MAX_TODOS_PER_CELL = 3;

    private static final String ACTION_PREV = "com.hyeongju.routineapp.SCHEDULE_PREV";
    private static final String ACTION_NEXT = "com.hyeongju.routineapp.SCHEDULE_NEXT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_PREV.equals(action) || ACTION_NEXT.equals(action)) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int idx = prefs.getInt(KEY_PAGE_INDEX, 1);
            int maxIdx = getPageCount(context) - 1;
            idx = ACTION_PREV.equals(action) ? Math.max(0, idx - 1) : Math.min(Math.max(0, maxIdx), idx + 1);
            prefs.edit().putInt(KEY_PAGE_INDEX, idx).apply();
            refreshAll(context);
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateOne(context, appWidgetManager, id);
        }
    }

    // WidgetBridgePlugin이 새 데이터를 받았을 때 즉시 다시 그리기 위해 호출.
    public static void refreshAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, ScheduleWidgetProvider.class));
        for (int id : ids) {
            updateOne(context, mgr, id);
        }
    }

    private static int idFor(Context context, String name) {
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }

    private static int getPageCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_SCHEDULE_DATA, null);
        if (raw == null) return 1;
        try {
            JSONArray pages = new JSONObject(raw).optJSONArray("pages");
            return pages != null ? Math.max(1, pages.length()) : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private static PendingIntent navPendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, ScheduleWidgetProvider.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void updateOne(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_schedule);

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(idFor(context, "widget_schedule_root"), openPending);

        // 배경을 XML의 @drawable/widget_background로 그냥 두면 홈 화면 런처가
        // "그 순간 자신의" 다크/라이트 상태로 실시간 재해석해서 그리는데, 글자색은
        // 우리 앱이 마지막으로 push한 순간의 다크/라이트 상태로 이미 확정돼 심어짐 —
        // 그 사이 시스템 다크/라이트가 바뀌면 배경만 새 상태로 바뀌고 글자색은
        // 예전 상태로 남아 어긋날 수 있음(예: 라이트로 바뀐 흰 배경 위에 다크 모드
        // 때 심어둔 흰 글자가 그대로 남아 안 보이는 사고). 배경도 글자색과 완전히
        // 같은 순간·같은 판단(isDark)으로 우리가 직접 골라 심어서 항상 맞게 함.
        boolean isDark = (context.getResources().getConfiguration().uiMode
            & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        views.setInt(idFor(context, "widget_schedule_root"), "setBackgroundResource",
            isDark ? R.drawable.widget_background_dark : R.drawable.widget_background_light);

        int primaryText = ContextCompat.getColor(context, R.color.widget_text_primary);
        int secondaryText = ContextCompat.getColor(context, R.color.widget_text_secondary);

        // 자료가 없거나 깨져 있어도 안전하게 빈 상태로 시작
        for (int i = 0; i < 7; i++) {
            views.setTextViewText(idFor(context, "sch_header_" + i), "");
            views.setTextColor(idFor(context, "sch_header_" + i), secondaryText);
        }
        for (int i = 0; i < 14; i++) {
            int dateId = idFor(context, "sch_date_" + i);
            views.setTextViewText(dateId, "");
            views.setTextColor(dateId, primaryText);
            views.setInt(idFor(context, "sch_cell_" + i), "setBackgroundColor", 0x00000000);
            views.setTextViewText(idFor(context, "sch_shift_" + i), "");
            views.setInt(idFor(context, "sch_shift_" + i), "setBackgroundColor", 0x00000000);
            for (int t = 0; t < MAX_TODOS_PER_CELL; t++) {
                views.setTextViewText(idFor(context, "sch_todo_" + i + "_" + t), "");
            }
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_SCHEDULE_DATA, null);

        // 화살표 글자 대신, 월요일 쪽 세로 전체(헤더+두 줄)를 누르면 이전 페이지,
        // 일요일 쪽 세로 전체를 누르면 다음 페이지로 넘어가게 함. 어느 칸이
        // 월/일요일인지는 주 시작 요일 설정에 따라 달라지므로 JS가 넘겨준 sunCol
        // 기준으로 계산(일요일 바로 다음 칸이 항상 월요일 — weekStart 무관하게
        // 성립하는 항등식). 데이터가 아직 없을 때는 기본값(월=0,일=6)으로 둠.
        int sunColForNav = 6;
        if (raw != null) {
            try {
                sunColForNav = new JSONObject(raw).optInt("sunCol", 6);
            } catch (Exception e) {
                // 무시 — 기본값 사용
            }
        }
        int mondayCol = (sunColForNav + 1) % 7;
        PendingIntent prevPending = navPendingIntent(context, ACTION_PREV, 3);
        PendingIntent nextPending = navPendingIntent(context, ACTION_NEXT, 4);
        views.setOnClickPendingIntent(idFor(context, "sch_header_" + mondayCol), prevPending);
        views.setOnClickPendingIntent(idFor(context, "sch_cell_" + mondayCol), prevPending);
        views.setOnClickPendingIntent(idFor(context, "sch_cell_" + (7 + mondayCol)), prevPending);
        views.setOnClickPendingIntent(idFor(context, "sch_header_" + sunColForNav), nextPending);
        views.setOnClickPendingIntent(idFor(context, "sch_cell_" + sunColForNav), nextPending);
        views.setOnClickPendingIntent(idFor(context, "sch_cell_" + (7 + sunColForNav)), nextPending);

        if (raw != null) {
            try {
                JSONObject obj = new JSONObject(raw);

                JSONArray headers = obj.optJSONArray("headers");
                int satCol = obj.optInt("satCol", -1);
                int sunCol = obj.optInt("sunCol", -1);
                if (headers != null) {
                    for (int i = 0; i < headers.length() && i < 7; i++) {
                        int hid = idFor(context, "sch_header_" + i);
                        views.setTextViewText(hid, headers.optString(i, ""));
                        if (i == satCol) views.setTextColor(hid, 0xFF007AFF);
                        else if (i == sunCol) views.setTextColor(hid, 0xFFFF3B30);
                    }
                }

                JSONArray pages = obj.optJSONArray("pages");
                if (pages != null && pages.length() > 0) {
                    int idx = prefs.getInt(KEY_PAGE_INDEX, 1);
                    if (idx < 0) idx = 0;
                    if (idx > pages.length() - 1) idx = pages.length() - 1;

                    JSONObject pageObj = pages.optJSONObject(idx);
                    if (pageObj != null) {
                        JSONArray days = pageObj.optJSONArray("days");
                        if (days != null) {
                            for (int i = 0; i < days.length() && i < 14; i++) {
                                Object dayObj = days.opt(i);
                                if (!(dayObj instanceof JSONObject)) continue;
                                JSONObject day = (JSONObject) dayObj;
                                int dayNum = day.optInt("dayNum", 0);
                                boolean isToday = day.optBoolean("isToday", false);
                                String shiftName = day.optString("shiftName", "");
                                String color = day.optString("color", "");
                                JSONArray todos = day.optJSONArray("todos");

                                int dateId = idFor(context, "sch_date_" + i);
                                int shiftId = idFor(context, "sch_shift_" + i);
                                int cellId = idFor(context, "sch_cell_" + i);

                                views.setTextViewText(dateId, String.valueOf(dayNum));
                                if (isToday) {
                                    views.setTextColor(dateId, 0xFF007AFF);
                                    // 오늘은 날짜 숫자만이 아니라 그 날 칸 전체에 테두리를 둘러서 표시.
                                    views.setInt(cellId, "setBackgroundResource", R.drawable.widget_today_cell_border);
                                }

                                if (!shiftName.isEmpty()) {
                                    views.setTextViewText(shiftId, shiftName);
                                    if (!color.isEmpty()) {
                                        try {
                                            int base = Color.parseColor(color);
                                            // 앱의 근무 배지(applyShiftBadgeColor)와 같은 느낌:
                                            // 배경은 옅게(약 15% 불투명도), 글자는 근무색 그대로.
                                            int tintedBg = (base & 0x00FFFFFF) | 0x26000000;
                                            views.setInt(shiftId, "setBackgroundColor", tintedBg);
                                            views.setTextColor(shiftId, base | 0xFF000000);
                                        } catch (IllegalArgumentException e) {
                                            // 색상 파싱 실패 시 배경 없이 글자만 표시
                                        }
                                    }
                                }

                                if (todos != null) {
                                    for (int t = 0; t < todos.length() && t < MAX_TODOS_PER_CELL; t++) {
                                        views.setTextViewText(idFor(context, "sch_todo_" + i + "_" + t), todos.optString(t, ""));
                                    }
                                }
                            }
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
