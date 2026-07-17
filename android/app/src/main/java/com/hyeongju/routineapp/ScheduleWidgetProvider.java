package com.hyeongju.routineapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
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

    // 달력 위젯과 같은 값(웹의 --holiday-color) — 자세한 이유는 그쪽 주석 참고.
    private static final int HOLIDAY_COLOR = 0xFFD9645E;

    private static CharSequence buildDateText(int dayNum, String holidayName) {
        String numStr = String.valueOf(dayNum);
        if (holidayName == null || holidayName.isEmpty()) return numStr;
        SpannableStringBuilder ssb = new SpannableStringBuilder(numStr + " " + holidayName);
        int start = numStr.length() + 1;
        int end = ssb.length();
        ssb.setSpan(new RelativeSizeSpan(0.62f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new ForegroundColorSpan(HOLIDAY_COLOR), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }

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

    // "7월 16일 (수)" 형태로 만듦 — dateStr(YYYY-MM-DD)에서 월/일만 뽑고,
    // 요일 이름은 이미 JS가 계산해 넘겨준 headers 배열(그 칸의 열 번호에 해당하는
    // 값)을 그대로 씀. 근무 계산이 아니라 문자열 가공이라 네이티브에 둬도 되는
    // 예외(다른 위젯들의 currentDisplayedMonth()와 같은 성격).
    private static String buildDateLabel(String dateStr, int dayNum, String weekdayName) {
        if (dateStr == null || dateStr.length() < 7) return dateStr;
        String month = dateStr.substring(5, 7);
        String label = (month.startsWith("0") ? month.substring(1) : month) + "월 " + dayNum + "일";
        if (weekdayName != null && !weekdayName.isEmpty()) label += " (" + weekdayName + ")";
        return label;
    }

    // 위젯이 지금 보여주고 있는 2주 페이지의 첫날을 기준으로 "그 달"(YYYY-MM)을
    // 뽑아서 앱을 열 때 달력 탭이 그 달로 바로 이동하게 함 — 달력 위젯의
    // currentDisplayedMonth()와 같은 방식(이미 JS가 계산해서 저장해둔 date
    // 문자열에서 잘라 쓰는 것뿐, 근무 계산 아님).
    private static String currentDisplayedMonth(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_SCHEDULE_DATA, null);
        if (raw == null) return null;
        try {
            JSONArray pages = new JSONObject(raw).optJSONArray("pages");
            if (pages == null || pages.length() == 0) return null;
            int idx = prefs.getInt(KEY_PAGE_INDEX, 1);
            if (idx < 0) idx = 0;
            if (idx > pages.length() - 1) idx = pages.length() - 1;
            JSONObject pageObj = pages.optJSONObject(idx);
            if (pageObj == null) return null;
            JSONArray days = pageObj.optJSONArray("days");
            if (days == null || days.length() == 0) return null;
            JSONObject firstDay = days.optJSONObject(0);
            if (firstDay == null) return null;
            String dateStr = firstDay.optString("date", "");
            return dateStr.length() >= 7 ? dateStr.substring(0, 7) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void updateOne(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_schedule);

        Intent openIntent = new Intent(context, MainActivity.class);
        // 위젯마다 다른 action을 붙여서 서로 다른 PendingIntent로 구분되게 함 —
        // 안 붙이면 다른 위젯들의 "MainActivity 열기" 인텐트와 requestCode+인텐트가
        // 같은 것으로 취급돼(extras는 구분 기준에 안 들어감) 전부 하나로 뭉쳐지고,
        // 가장 마지막에 갱신된 위젯의 목적지로만 열리는 버그가 있었음(2026-07-15
        // 수정, 달력 위젯 항목 참고).
        openIntent.setAction("com.hyeongju.routineapp.OPEN_APP_SCHEDULE");
        openIntent.putExtra(MainActivity.EXTRA_WIDGET_NAV, "month");
        String targetMonth = currentDisplayedMonth(context);
        if (targetMonth != null) openIntent.putExtra(MainActivity.EXTRA_WIDGET_NAV_MONTH, targetMonth);
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
        boolean isDark = WidgetThemeHelper.isDarkMode(context);
        views.setInt(idFor(context, "widget_schedule_root"), "setBackgroundResource",
            isDark ? R.drawable.widget_background_dark : R.drawable.widget_background_light);

        int primaryText = WidgetThemeHelper.primaryTextColor(context);
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

        // 좌우 넘기기 영역(2026-07-16 세 번째 조정 — 위치(왼쪽 끝/오른쪽 끝)
        // 기준으로 단순화): 예전엔 "월요일 칸"·"일요일 칸"처럼 요일 이름 기준
        // (mondayCol/sunColForNav, weekStart에 따라 달라짐)으로 넘기기 칸을
        // 정했는데, 사용자가 "제일 왼쪽 칸/제일 오른쪽 칸"이라고 위치로 다시
        // 요청해서 요일 이름과 상관없이 항상 열 번호 0(왼쪽 끝)/6(오른쪽 끝)
        // 고정으로 바꿈 — 더 이상 sunCol을 안 읽어도 됨. 헤더 칸(sch_header_0/6)은
        // 그대로 넘기기 전용.
        PendingIntent prevPending = navPendingIntent(context, ACTION_PREV, 3);
        PendingIntent nextPending = navPendingIntent(context, ACTION_NEXT, 4);
        views.setOnClickPendingIntent(idFor(context, "sch_header_0"), prevPending);
        views.setOnClickPendingIntent(idFor(context, "sch_header_6"), nextPending);

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
                            // 진짜 근무 일괄 수정(같은 batchId)으로 이어진 날짜만 글자를 첫
                            // 칸에만 쓰고 나머지는 색만 이어붙임(2026-07-18 추가 — 예전엔
                            // 이런 구분이 아예 없어서 "괌"처럼 여러 날을 한 번에 바꾼 근무가
                            // 모든 칸에 그대로 반복 표시되는 문제가 있었음. 달력 위젯·앱
                            // 화면의 appendShiftIndicator와 같은 기준으로 통일). 줄(=주)이
                            // 바뀌면 이어짐 판단을 초기화.
                            String prevBatchId = null;
                            for (int i = 0; i < days.length() && i < 14; i++) {
                                if (i % 7 == 0) prevBatchId = null;
                                Object dayObj = days.opt(i);
                                if (!(dayObj instanceof JSONObject)) { prevBatchId = null; continue; }
                                JSONObject day = (JSONObject) dayObj;
                                String dateStr = day.optString("date", "");
                                int dayNum = day.optInt("dayNum", 0);
                                boolean isToday = day.optBoolean("isToday", false);
                                String shiftName = day.optString("shiftName", "");
                                String color = day.optString("color", "");
                                String batchId = day.optString("batchId", "");
                                String holidayName = day.optString("holidayName", "");
                                JSONArray todos = day.optJSONArray("todos");

                                int dateId = idFor(context, "sch_date_" + i);
                                int shiftId = idFor(context, "sch_shift_" + i);
                                int cellId = idFor(context, "sch_cell_" + i);

                                views.setTextViewText(dateId, buildDateText(dayNum, holidayName));
                                if (isToday) {
                                    views.setTextColor(dateId, 0xFF007AFF);
                                    // 오늘은 날짜 숫자만이 아니라 그 날 칸 전체에 테두리를 둘러서 표시.
                                    views.setInt(cellId, "setBackgroundResource", R.drawable.widget_today_cell_border);
                                }

                                if (!shiftName.isEmpty()) {
                                    boolean continuesBand = !batchId.isEmpty() && batchId.equals(prevBatchId);
                                    if (!continuesBand) {
                                        views.setTextViewText(shiftId, shiftName);
                                    } // 이어지는 칸은 글자 없이 색만(위에서 이미 ""로 비워둔 상태)
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
                                prevBatchId = batchId.isEmpty() ? null : batchId;

                                if (todos != null) {
                                    for (int t = 0; t < todos.length() && t < MAX_TODOS_PER_CELL; t++) {
                                        views.setTextViewText(idFor(context, "sch_todo_" + i + "_" + t), todos.optString(t, ""));
                                    }
                                }

                                // 팝업/넘기기 영역 재조정(2026-07-16 네 번째): 모든 칸에서
                                // "날짜 숫자(sch_date_N)"와 "근무 배지(sch_shift_N)"는
                                // 항상 팝업으로 통일(위/아래 줄 다 똑같이, 왼쪽 끝(col 0)·
                                // 오른쪽 끝(col 6)도 예외 없음) — 처음엔 숫자만 팝업이고
                                // 나머지 전체가 넘기기였는데, 사용자가 "숫자랑 밑에 근무
                                // 까지는 영역으로"(=팝업으로) 요청해서 근무 배지도 팝업
                                // 쪽으로 넘어옴. 그 결과 왼쪽·오른쪽 끝 칸에서 넘기기로
                                // 남는 부분은 할일 줄(sch_todo_N_0~2)뿐 — 이 줄들은 따로
                                // 클릭을 안 걸어서 자식이 없는 자리로 남고, 그 자리는
                                // cellId(칸 전체, 부모)가 받음. sch_date_N·sch_shift_N이
                                // sch_cell_N 안에 중첩된 자식 뷰라 안드로이드 표준 터치
                                // 처리로 그 둘 위는 항상 자식(팝업)이 받고, 자식이 없는
                                // 나머지 자리(할일 줄 + 여백)는 부모(cellId)가 받음 —
                                // 위젯 안 목록(RemoteViewsFactory)과 달리 보통 레이아웃이라
                                // fillInIntent 같은 별도 처리 없이 이 분리가 그대로 됨.
                                // 가운데 칸은 cellId도 그냥 팝업이라 전체가 다 팝업.
                                int col = i % 7;
                                if (!dateStr.isEmpty()) {
                                    Intent dayIntent = new Intent(context, DayQuickViewActivity.class);
                                    dayIntent.setAction("com.hyeongju.routineapp.OPEN_DAY_" + dateStr);
                                    dayIntent.putExtra(DayQuickViewActivity.EXTRA_DATE, dateStr);
                                    String weekdayName = headers != null ? headers.optString(col, "") : "";
                                    dayIntent.putExtra(DayQuickViewActivity.EXTRA_DATE_LABEL,
                                        buildDateLabel(dateStr, dayNum, weekdayName));
                                    if (!shiftName.isEmpty()) {
                                        dayIntent.putExtra(DayQuickViewActivity.EXTRA_SHIFT_NAME, shiftName);
                                        if (!color.isEmpty()) dayIntent.putExtra(DayQuickViewActivity.EXTRA_SHIFT_COLOR, color);
                                    }
                                    if (todos != null) dayIntent.putExtra(DayQuickViewActivity.EXTRA_TODOS, todos.toString());
                                    PendingIntent dayPending = PendingIntent.getActivity(
                                        context, 200 + i, dayIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                    );
                                    views.setOnClickPendingIntent(dateId, dayPending);
                                    views.setOnClickPendingIntent(shiftId, dayPending);
                                    if (col == 0) {
                                        views.setOnClickPendingIntent(cellId, prevPending);
                                    } else if (col == 6) {
                                        views.setOnClickPendingIntent(cellId, nextPending);
                                    } else {
                                        views.setOnClickPendingIntent(cellId, dayPending);
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
