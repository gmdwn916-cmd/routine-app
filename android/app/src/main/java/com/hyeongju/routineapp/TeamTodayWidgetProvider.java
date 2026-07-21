package com.hyeongju.routineapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

// 오늘 팀 근무 위젯(위젯 6, 2026-07-21 추가) — 4x1 크기, 오늘 하루 모든 팀의
// 근무를 한 줄로 보여주는 읽기 전용 위젯. 다른 위젯들과 같은 원칙(근무 계산은
// 전부 JS가 끝내고, 여기서는 결과(JSON)를 그대로 그리기만 함 — 팀 근무 계산
// 로직(getOrderedTeams/getTeamShift)이 두 곳에 있으면 어긋날 위험이 있어서
// 네이티브에는 절대 안 둠). 팀 칸은 최대 MAX_TEAMS개까지 미리 선언해둔 걸
// 재활용 — RemoteViews는 뷰를 동적으로 늘릴 수 없어서(다른 위젯들과 같은 이유).
public class TeamTodayWidgetProvider extends AppWidgetProvider {
    public static final String PREFS_NAME = "widget_bridge";
    public static final String KEY_TEAM_TODAY_DATA = "team_today_widget_data";

    private static final int MAX_TEAMS = 6;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateOne(context, appWidgetManager, id);
        }
    }

    // 휴대폰 시스템 다크/라이트 설정이 바뀌면 앱을 안 열어도 위젯을 새로
    // 그림(다른 위젯들과 같은 이유, "위젯 공통 규칙" 참고).
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction())) {
            refreshAll(context);
            return;
        }
        super.onReceive(context, intent);
    }

    // WidgetBridgePlugin이 새 데이터를 받았을 때 즉시 다시 그리기 위해 호출.
    public static void refreshAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, TeamTodayWidgetProvider.class));
        for (int id : ids) {
            updateOne(context, mgr, id);
        }
    }

    private static int idFor(Context context, String name) {
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }

    private static void updateOne(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_team_today);

        // 위젯을 탭하면 "다른 팀 근무 확인" 화면으로 바로 이동(다른 위젯들과
        // 같은 "네이티브는 저장만, JS가 다음 동기화 시점에 가져감" 패턴 —
        // widget_nav="teamcal"). 다른 위젯들의 "앱 열기" 인텐트와 안 겹치게
        // 고유한 action 문자열을 붙임(위 "위젯 공통 규칙" 참고).
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setAction("com.hyeongju.routineapp.OPEN_APP_TEAM_TODAY");
        openIntent.putExtra(MainActivity.EXTRA_WIDGET_NAV, "teamcal");
        PendingIntent openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(idFor(context, "widget_team_today_root"), openPending);

        boolean isDark = WidgetThemeHelper.isDarkMode(context);
        views.setInt(idFor(context, "widget_team_today_root"), "setBackgroundResource",
            isDark ? R.drawable.widget_background_dark : R.drawable.widget_background_light);

        int primaryText = WidgetThemeHelper.primaryTextColor(context);
        int secondaryText = ContextCompat.getColor(context, R.color.widget_text_secondary);
        int accentBlue = 0xFF007AFF;

        // 자료가 없거나 깨져 있어도 안전하게 빈 상태로 시작
        views.setTextViewText(idFor(context, "tt_date"), "");
        views.setTextColor(idFor(context, "tt_date"), primaryText);
        for (int i = 0; i < MAX_TEAMS; i++) {
            views.setViewVisibility(idFor(context, "tt_slot_" + i), View.GONE);
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_TEAM_TODAY_DATA, null);
        if (raw != null) {
            try {
                JSONObject obj = new JSONObject(raw);
                views.setTextViewText(idFor(context, "tt_date"), obj.optString("dateLabel", ""));

                JSONArray teams = obj.optJSONArray("teams");
                if (teams != null) {
                    for (int i = 0; i < teams.length() && i < MAX_TEAMS; i++) {
                        JSONObject team = teams.optJSONObject(i);
                        if (team == null) continue;

                        int slotId = idFor(context, "tt_slot_" + i);
                        int nameId = idFor(context, "tt_name_" + i);
                        int shiftId = idFor(context, "tt_shift_" + i);
                        views.setViewVisibility(slotId, View.VISIBLE);

                        boolean isMyTeam = team.optBoolean("isMyTeam", false);
                        views.setTextViewText(nameId, team.optString("name", ""));
                        views.setTextColor(nameId, isMyTeam ? accentBlue : secondaryText);

                        String shiftName = team.optString("shiftName", "");
                        String color = team.optString("color", "");
                        views.setTextViewText(shiftId, shiftName.isEmpty() ? "—" : shiftName);
                        if (!color.isEmpty()) {
                            try {
                                int base = Color.parseColor(color);
                                // 근무 배지 색 레시피(applyShiftBadgeColor)와 같은 톤 —
                                // 배경은 옅게(15%), 글자는 근무색 그대로.
                                int tintedBg = (base & 0x00FFFFFF) | 0x26000000;
                                views.setInt(shiftId, "setBackgroundColor", tintedBg);
                                views.setTextColor(shiftId, base | 0xFF000000);
                            } catch (IllegalArgumentException e) {
                                views.setInt(shiftId, "setBackgroundColor", 0x00000000);
                                views.setTextColor(shiftId, secondaryText);
                            }
                        } else {
                            views.setInt(shiftId, "setBackgroundColor", 0x00000000);
                            views.setTextColor(shiftId, secondaryText);
                        }
                    }
                }
            } catch (Exception e) {
                // 데이터가 깨져 있으면 위에서 이미 비워둔 상태로 둠
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
