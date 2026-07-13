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

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

// 이번 달 달력을 앱의 달력 탭과 같은 모양(날짜 숫자 + 근무이름 작은 배지)으로
// 보여주는 읽기 전용 위젯 + 이전/다음 달 넘기기. 근무 계산은 전혀 모름 —
// 웹(JS)이 이미 계산해서 WidgetBridgePlugin.setMonthCalendarData()로 넘겨준
// [지난달,이번달,다음달] 3개월치 데이터(SharedPreferences에 JSON으로 저장됨)를
// 그대로 그리기만 함. 요일 순서·토/일 열 번호도 전부 JS가 알려줌 — 여기서 새로
// 계산하는 건 "오늘" 강조뿐(날짜 문자열 비교라 근무 로직 아님 — 중복 구현 아님).
public class MonthCalendarWidgetProvider extends AppWidgetProvider {
    public static final String PREFS_NAME = "widget_bridge";
    public static final String KEY_MONTH_DATA = "month_calendar_data";
    public static final String KEY_DISPLAY_INDEX = "month_calendar_display_index";
    private static final int CENTER_INDEX = 1; // months 배열은 항상 [지난달, 이번달, 다음달] 고정 순서

    private static final String ACTION_PREV = "com.hyeongju.routineapp.WIDGET_MONTH_PREV";
    private static final String ACTION_NEXT = "com.hyeongju.routineapp.WIDGET_MONTH_NEXT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_PREV.equals(action) || ACTION_NEXT.equals(action)) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int idx = prefs.getInt(KEY_DISPLAY_INDEX, CENTER_INDEX);
            idx = ACTION_PREV.equals(action) ? Math.max(0, idx - 1) : Math.min(2, idx + 1);
            prefs.edit().putInt(KEY_DISPLAY_INDEX, idx).apply();
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

    // WidgetBridgePlugin이 새 데이터를 받았을 때, 또는 이전/다음 달 버튼을
    // 눌렀을 때 즉시 다시 그리기 위해 호출.
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

    private static PendingIntent navPendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, MonthCalendarWidgetProvider.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void updateOne(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_month_calendar);

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(idFor(context, "widget_month_root"), openPending);

        // 화살표 글자 대신, 월요일 쪽 세로 전체(헤더+6줄 그리드)를 누르면 이전
        // 달, 일요일 쪽 세로 전체를 누르면 다음 달로 넘어가게 함(스케줄 위젯과
        // 동일한 방식) — 어느 열이 월/일요일인지는 JS가 넘겨준 sunCol로 계산
        // ("일요일 바로 다음 칸이 항상 월요일"이라는 항등식, weekStart 무관).
        int sunColForNav = 6;
        {
            SharedPreferences navPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String navRaw = navPrefs.getString(KEY_MONTH_DATA, null);
            if (navRaw != null) {
                try {
                    sunColForNav = new JSONObject(navRaw).optInt("sunCol", 6);
                } catch (Exception e) {
                    // 무시 — 기본값 사용
                }
            }
        }
        int mondayCol = (sunColForNav + 1) % 7;
        PendingIntent prevPending = navPendingIntent(context, ACTION_PREV, 1);
        PendingIntent nextPending = navPendingIntent(context, ACTION_NEXT, 2);
        views.setOnClickPendingIntent(idFor(context, "header_" + mondayCol), prevPending);
        views.setOnClickPendingIntent(idFor(context, "header_" + sunColForNav), nextPending);
        for (int row = 0; row < 6; row++) {
            views.setOnClickPendingIntent(idFor(context, "cell_container_" + (row * 7 + mondayCol)), prevPending);
            views.setOnClickPendingIntent(idFor(context, "cell_container_" + (row * 7 + sunColForNav)), nextPending);
        }

        int primaryText = ContextCompat.getColor(context, R.color.widget_text_primary);
        int secondaryText = ContextCompat.getColor(context, R.color.widget_text_secondary);

        // 자료가 없거나 깨져 있어도 안전하게 빈 칸으로 시작
        for (int i = 0; i < 7; i++) {
            views.setTextViewText(idFor(context, "header_" + i), "");
            views.setTextColor(idFor(context, "header_" + i), secondaryText);
        }
        for (int i = 0; i < 42; i++) {
            views.setTextViewText(idFor(context, "cell_date_" + i), "");
            views.setTextViewText(idFor(context, "cell_shift_" + i), "");
            views.setTextColor(idFor(context, "cell_date_" + i), primaryText);
            views.setInt(idFor(context, "cell_shift_" + i), "setBackgroundColor", 0x00000000);
        }
        views.setTextViewText(idFor(context, "widget_month_label"), "");

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_MONTH_DATA, null);

        if (raw != null) {
            try {
                JSONObject obj = new JSONObject(raw);

                JSONArray headers = obj.optJSONArray("headers");
                int satCol = obj.optInt("satCol", -1);
                int sunCol = obj.optInt("sunCol", -1);
                if (headers != null) {
                    for (int i = 0; i < headers.length() && i < 7; i++) {
                        int hid = idFor(context, "header_" + i);
                        views.setTextViewText(hid, headers.optString(i, ""));
                        if (i == satCol) views.setTextColor(hid, 0xFF007AFF);
                        else if (i == sunCol) views.setTextColor(hid, 0xFFFF3B30);
                    }
                }

                JSONArray months = obj.optJSONArray("months");
                if (months != null && months.length() > 0) {
                    int idx = prefs.getInt(KEY_DISPLAY_INDEX, CENTER_INDEX);
                    if (idx < 0) idx = 0;
                    if (idx > months.length() - 1) idx = months.length() - 1;

                    JSONObject monthObj = months.optJSONObject(idx);
                    if (monthObj != null) {
                        views.setTextViewText(idFor(context, "widget_month_label"), monthObj.optString("monthLabel", ""));

                        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());

                        JSONArray days = monthObj.optJSONArray("days");
                        if (days != null) {
                            // 같은 근무가 옆 칸(같은 줄=같은 주)까지 연속되면 배경 띠는 계속
                            // 이어붙이되, 글자는 그 연속의 첫 칸에만 씀(괌 여행처럼 기간
                            // 할일을 이어진 띠 하나로 보여주던 것과 같은 원리) — 줄이 바뀌면
                            // 다른 주라서 새로 시작(이어짐 판단 초기화).
                            String prevShiftInRow = null;
                            for (int i = 0; i < days.length() && i < 42; i++) {
                                if (i % 7 == 0) prevShiftInRow = null;

                                Object dayObj = days.opt(i);
                                if (!(dayObj instanceof JSONObject)) { prevShiftInRow = null; continue; } // 그 달에 속하지 않는 빈 칸
                                JSONObject day = (JSONObject) dayObj;
                                String dateStr = day.optString("date", "");
                                int dayNum = day.optInt("dayNum", 0);
                                String shiftName = day.optString("shiftName", "");
                                String color = day.optString("color", "");

                                int dateId = idFor(context, "cell_date_" + i);
                                int shiftId = idFor(context, "cell_shift_" + i);

                                views.setTextViewText(dateId, String.valueOf(dayNum));

                                if (!shiftName.isEmpty()) {
                                    boolean continuesBand = shiftName.equals(prevShiftInRow);
                                    if (!continuesBand) {
                                        views.setTextViewText(shiftId, shiftName);
                                    } // 이어지는 칸은 글자 없이 배경 띠만(위에서 이미 ""로 비워둔 상태)
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
                                prevShiftInRow = shiftName.isEmpty() ? null : shiftName;

                                if (dateStr.equals(todayStr)) {
                                    views.setTextColor(dateId, 0xFF007AFF);
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
