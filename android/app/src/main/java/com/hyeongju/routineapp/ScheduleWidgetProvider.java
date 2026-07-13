package com.hyeongju.routineapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

// N주(최대 2주) 스케줄을 세로로 보여주는 읽기 전용 위젯. 위젯 2(달력)와 완전히
// 같은 원칙 — 근무 계산·할 일 목록 판단은 전부 JS(index.html)가 끝내고 결과만
// 넘겨줌(WidgetBridgePlugin.setScheduleData). 여기서 하는 일은 그 결과를
// 위젯 크기에 맞는 줄 수만큼 채워 넣는 것뿐 — 근무·할일 로직 없음.
public class ScheduleWidgetProvider extends AppWidgetProvider {
    public static final String PREFS_NAME = "widget_bridge";
    public static final String KEY_SCHEDULE_DATA = "schedule_widget_data";

    private static final int MAX_ROWS = 14;
    private static final int MIN_ROWS = 7;
    private static final int HEADER_PADDING_DP = 12; // 위젯 자체 padding(6dp) * 2
    private static final int ROW_HEIGHT_DP = 26; // 한 줄 대략 높이(추정치 — 실기기 보고 조정 가능)

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateOne(context, appWidgetManager, id);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        updateOne(context, appWidgetManager, appWidgetId);
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

    private static void updateOne(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_schedule);

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(idFor(context, "widget_schedule_root"), openPending);

        // 위젯 실제 크기(세로 높이)에 맞춰 몇 줄을 보여줄지 정함 — 작게 놓으면
        // 1주(7줄), 세로로 늘리면 최대 2주(14줄)까지 자동으로 늘어남.
        int minHeightDp = 0;
        try {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        } catch (Exception e) {
            // 옵션을 못 가져오면 기본값(1주)으로 둠
        }
        int rowsAvailable = (minHeightDp - HEADER_PADDING_DP) / ROW_HEIGHT_DP;
        int rowsToShow = Math.max(MIN_ROWS, Math.min(MAX_ROWS, rowsAvailable > 0 ? rowsAvailable : MIN_ROWS));

        int primaryText = ContextCompat.getColor(context, R.color.widget_text_primary);
        int secondaryText = ContextCompat.getColor(context, R.color.widget_text_secondary);

        // 자료가 없거나 깨져 있어도 안전하게 빈 상태로 시작
        for (int i = 0; i < MAX_ROWS; i++) {
            views.setViewVisibility(idFor(context, "row_container_" + i), i < rowsToShow ? View.VISIBLE : View.GONE);
            views.setTextViewText(idFor(context, "row_date_" + i), "");
            views.setTextViewText(idFor(context, "row_shift_" + i), "");
            views.setTextViewText(idFor(context, "row_todo_" + i), "");
            views.setTextColor(idFor(context, "row_date_" + i), primaryText);
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_SCHEDULE_DATA, null);

        if (raw != null) {
            try {
                JSONObject obj = new JSONObject(raw);
                JSONArray days = obj.optJSONArray("days");
                if (days != null) {
                    for (int i = 0; i < days.length() && i < rowsToShow; i++) {
                        Object dayObj = days.opt(i);
                        if (!(dayObj instanceof JSONObject)) continue;
                        JSONObject day = (JSONObject) dayObj;
                        String label = day.optString("label", "");
                        String shiftName = day.optString("shiftName", "");
                        String color = day.optString("color", "");
                        String todoSummary = day.optString("todoSummary", "");
                        boolean isToday = day.optBoolean("isToday", false);

                        int dateId = idFor(context, "row_date_" + i);
                        int shiftId = idFor(context, "row_shift_" + i);
                        int todoId = idFor(context, "row_todo_" + i);

                        views.setTextViewText(dateId, label);
                        views.setTextColor(dateId, isToday ? 0xFF007AFF : primaryText);

                        views.setTextViewText(shiftId, shiftName);
                        if (!color.isEmpty()) {
                            try {
                                int base = Color.parseColor(color);
                                views.setTextColor(shiftId, base | 0xFF000000);
                            } catch (IllegalArgumentException e) {
                                views.setTextColor(shiftId, secondaryText);
                            }
                        } else {
                            views.setTextColor(shiftId, secondaryText);
                        }

                        views.setTextViewText(todoId, todoSummary);
                    }
                }
            } catch (Exception e) {
                // 저장된 데이터가 깨져 있으면 위에서 이미 비워둔 상태로 둠
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
