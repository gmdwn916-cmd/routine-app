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

    // 공휴일 이름 색(2026-07-18 추가) — 웹의 --holiday-color(#d9645e)와 같은 값.
    // 채도를 낮춘 톤이라 전체 위젯 색조와 안 튐(원래 쨍한 빨강 #ff3b30이었다가
    // "전체 톤과 어울리게" 요청으로 낮춤).
    private static final int HOLIDAY_COLOR = 0xFFD9645E;

    // 날짜 칸의 숫자 뒤에 공휴일 이름을 작게 이어붙임(2026-07-18 추가) — 이
    // 위젯 레이아웃엔 공휴일 전용 칸이 따로 없어서, 같은 TextView 안에 크기·색이
    // 다른 부분(Span)을 넣는 방식으로 처리(레이아웃 XML은 안 건드림).
    // showMonth가 true면 "7/21"처럼 월을 같이 붙임(2026-07-22 추가, 스케줄
    // 위젯의 buildDateText와 같은 방식) — 맨 위 월 표시(widget_month_label)를
    // 없애는 대신, 이 그리드가 실제로 보여주는 첫 칸(index 0, 다른 달 날짜가
    // 삐져나온 자리일 수 있음)과 달이 넘어가는 칸(dayNum==1) 둘 다 이 조건에
    // 해당해서 화면 어딘가에 항상 "지금 몇 월인지"가 보임.
    private static CharSequence buildDateText(int dayNum, int monthNum, boolean showMonth, String holidayName) {
        String numStr = showMonth ? (monthNum + "/" + dayNum) : String.valueOf(dayNum);
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
            int idx = prefs.getInt(KEY_DISPLAY_INDEX, CENTER_INDEX);
            idx = ACTION_PREV.equals(action) ? Math.max(0, idx - 1) : Math.min(2, idx + 1);
            prefs.edit().putInt(KEY_DISPLAY_INDEX, idx).apply();
            refreshAll(context);
            return;
        }
        // 휴대폰 시스템 다크/라이트 설정이 바뀌면 앱을 안 열어도 위젯을 새로
        // 그림(2026-07-18 추가) — WidgetThemeHelper.isDarkMode()가 매번 다시
        // 판단하므로 refreshAll()만 다시 부르면 됨.
        if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
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

    // 위젯이 지금 보여주고 있는 달(이전/다음 달로 넘겨봤을 수 있음)을 앱을 열 때
    // 그대로 이어서 보여주기 위해, 그 달의 날짜 하나를 뽑아 "YYYY-MM"만 돌려줌.
    // 근무 계산이 아니라 이미 JS가 계산해서 저장해둔 데이터에서 문자열만 잘라
    // 쓰는 것이라 네이티브에 로직을 새로 두는 게 아님.
    private static String currentDisplayedMonth(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_MONTH_DATA, null);
        if (raw == null) return null;
        try {
            JSONArray months = new JSONObject(raw).optJSONArray("months");
            if (months == null || months.length() == 0) return null;
            int idx = prefs.getInt(KEY_DISPLAY_INDEX, CENTER_INDEX);
            if (idx < 0) idx = 0;
            if (idx > months.length() - 1) idx = months.length() - 1;
            JSONObject monthObj = months.optJSONObject(idx);
            if (monthObj == null) return null;
            JSONArray days = monthObj.optJSONArray("days");
            if (days == null) return null;
            for (int i = 0; i < days.length(); i++) {
                Object dayObj = days.opt(i);
                if (dayObj instanceof JSONObject) {
                    String dateStr = ((JSONObject) dayObj).optString("date", "");
                    if (dateStr.length() >= 7) return dateStr.substring(0, 7);
                }
            }
        } catch (Exception e) {
            // 무시 — 못 구하면 그냥 앱이 원래 열리던 대로 열림(오늘 기준)
        }
        return null;
    }

    private static void updateOne(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_month_calendar);

        Intent openIntent = new Intent(context, MainActivity.class);
        // 다른 위젯들(스케줄/오늘/미배치)도 전부 "MainActivity를 requestCode 0으로
        // 연다"는 같은 모양의 인텐트를 썼었는데, 안드로이드는 PendingIntent를
        // requestCode+인텐트(추가로 넣은 값(extras)은 안 봄)로 같은 것인지 판단해서,
        // 이 넷이 전부 "같은 PendingIntent"로 취급되고 있었음 — 그래서 위젯 중
        // 아무거나 눌러도 실제로는 가장 마지막에 갱신된 위젯(미배치)이 심어둔 목적지로만
        // 열리는 버그가 있었음(2026-07-15 수정). action을 위젯마다 다르게 붙여서
        // 서로 다른 PendingIntent로 구분되게 함.
        openIntent.setAction("com.hyeongju.routineapp.OPEN_APP_MONTH");
        openIntent.putExtra(MainActivity.EXTRA_WIDGET_NAV, "month");
        String targetMonth = currentDisplayedMonth(context);
        if (targetMonth != null) openIntent.putExtra(MainActivity.EXTRA_WIDGET_NAV_MONTH, targetMonth);
        PendingIntent openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(idFor(context, "widget_month_root"), openPending);

        // 배경을 XML의 @drawable/widget_background(안에서 @color/widget_bg를 참조)로
        // 그냥 두면, 그 색은 이 위젯을 그리는 홈 화면 런처가 "런처 자신의 그 순간
        // 다크/라이트 상태"로 실시간으로 다시 해석해서 그림 — 반면 글자색(아래
        // primaryText 등)은 우리 앱이 마지막으로 push했을 때의 다크/라이트 상태로
        // 이미 확정된 값을 그대로 심어서 보냄. 그래서 마지막 push 이후에 시스템
        // 다크/라이트가 바뀌면 배경은 새 상태로 바로 바뀌는데 글자색은 예전 상태
        // 그대로 남아서(예: 라이트로 바뀐 배경 위에 다크 모드 때 심어둔 흰 글자가
        // 그대로 남아 안 보이는 사고) 서로 어긋날 수 있음. 그래서 배경도 글자색과
        // 완전히 같은 순간·같은 판단(isDark)으로 우리가 직접 골라서 심어버림 —
        // 이러면 최소한 배경과 글자는 항상 같은 상태로 맞아 있음이 보장됨.
        boolean isDark = WidgetThemeHelper.isDarkMode(context);
        views.setInt(idFor(context, "widget_month_root"), "setBackgroundResource",
            isDark ? R.drawable.widget_background_dark : R.drawable.widget_background_light);

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
        // 2026-07-22 재조정: 6줄(42칸)이 아니라 5줄(35칸)만 보여줌(레이아웃도
        // 같이 수정, 자세한 이유는 widget_month_calendar.xml 상단 주석 참고).
        for (int row = 0; row < 5; row++) {
            views.setOnClickPendingIntent(idFor(context, "cell_container_" + (row * 7 + mondayCol)), prevPending);
            views.setOnClickPendingIntent(idFor(context, "cell_container_" + (row * 7 + sunColForNav)), nextPending);
        }

        int primaryText = WidgetThemeHelper.primaryTextColor(context);
        int secondaryText = ContextCompat.getColor(context, R.color.widget_text_secondary);

        // 자료가 없거나 깨져 있어도 안전하게 빈 칸으로 시작
        for (int i = 0; i < 7; i++) {
            views.setTextViewText(idFor(context, "header_" + i), "");
            views.setTextColor(idFor(context, "header_" + i), secondaryText);
        }
        for (int i = 0; i < 35; i++) {
            views.setTextViewText(idFor(context, "cell_date_" + i), "");
            views.setTextViewText(idFor(context, "cell_shift_" + i), "");
            views.setTextColor(idFor(context, "cell_date_" + i), primaryText);
            views.setInt(idFor(context, "cell_shift_" + i), "setBackgroundColor", 0x00000000);
            // 오늘 표시 테두리(widget_today_cell_border) 리셋 — 이게 없으면
            // 어제까지 "오늘"이었던 칸이 오늘이 아니게 된 뒤에도 테두리가 그대로
            // 남아있음(RemoteViews는 매번 지정한 속성만 다시 적용하고, 지정 안 한
            // 속성은 이전 상태를 그대로 유지함 — 날짜 칸마다 매번 새로 그릴 때
            // 테두리를 명시적으로 지워주지 않으면 예전에 테두리 있던 칸에 계속
            // 남음, 2026-07-16 발견·수정). 스케줄 위젯(sch_cell_N)은 처음부터
            // 이 리셋이 있었음 — 이 위젯만 빠져있던 것.
            views.setInt(idFor(context, "cell_container_" + i), "setBackgroundColor", 0x00000000);
            // 다른 달 날짜 흐림 표시(otherMonth, 2026-07-22 추가) 리셋 — 매번
            // 명시적으로 1.0(완전 불투명)으로 되돌려둔 뒤, 아래 데이터 채우는
            // 루프에서 otherMonth인 칸만 다시 흐리게 함(재활용되는 뷰가 이전
            // 칸의 흐림 상태를 그대로 물고 있는 사고 예방 — 이 위젯의 다른
            // "매번 리셋" 습관과 동일한 이유).
            views.setFloat(idFor(context, "cell_container_" + i), "setAlpha", 1.0f);
        }

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
                        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());

                        JSONArray days = monthObj.optJSONArray("days");
                        if (days != null) {
                            // 진짜 근무 일괄 수정(같은 batchId)으로 이어진 날짜만 배경 띠를
                            // 이어붙이고 글자는 그 구간 첫 칸에만 씀(2026-07-18 수정 — 예전엔
                            // "이름이 같으면" 이어붙였는데, 그러면 하루씩 따로 바꾼 날짜나
                            // 기본 패턴이 우연히 같은 이름을 반복할 때도 잘못 합쳐졌음 — 앱의
                            // appendShiftIndicator와 같은 기준으로 통일). **줄(주)이 바뀔 때
                            // 강제로 초기화하지 않음(2026-07-18 재수정)** — days 배열은 항상
                            // 날짜순이라 배열의 바로 앞 칸이 곧 "어제"이므로, 토요일에서
                            // 일요일로(줄이 바뀌는 지점) 넘어가며 이어진 일괄 수정도 배열
                            // 인덱스만 보면 그대로 이어짐 — 줄마다 초기화하면 그 경계에서만
                            // 이름이 다시 튀어나오는 버그가 있었음(앱 화면은 이런 "줄" 개념
                            // 자체가 없이 실제 어제 날짜만 보므로 이 버그가 없었음). 그 달에
                            // 안 속하는 빈 칸(null)을 만나면 거기서만 진짜로 끊긴 것이므로
                            // 그때만 초기화.
                            String prevBatchId = null;
                            // 5줄(35칸)까지만 그림 — JS는 여전히 42칸(6주)을
                            // 계산해 넘기지만, 이 위젯 화면 자체는 5줄만 있으므로
                            // 나머지는 무시(위 "6줄→5줄" 재조정 참고).
                            for (int i = 0; i < days.length() && i < 35; i++) {
                                Object dayObj = days.opt(i);
                                if (!(dayObj instanceof JSONObject)) { prevBatchId = null; continue; } // 그 달에 속하지 않는 빈 칸
                                JSONObject day = (JSONObject) dayObj;
                                String dateStr = day.optString("date", "");
                                int dayNum = day.optInt("dayNum", 0);
                                String shiftName = day.optString("shiftName", "");
                                String color = day.optString("color", "");
                                String batchId = day.optString("batchId", "");
                                String holidayName = day.optString("holidayName", "");
                                boolean otherMonth = day.optBoolean("otherMonth", false);

                                int dateId = idFor(context, "cell_date_" + i);
                                int shiftId = idFor(context, "cell_shift_" + i);
                                int cellId = idFor(context, "cell_container_" + i);

                                // 맨 위 월 표시(widget_month_label)를 없앤 대신, 이 그리드가
                                // 실제로 보여주는 첫 칸(index 0 — 다른 달 날짜가 삐져나온
                                // 자리일 수 있음)과 달이 넘어가는 칸(dayNum==1) 둘 다 "7/1"
                                // 처럼 월을 같이 보여줌(스케줄 위젯과 같은 방식, 2026-07-22
                                // 추가).
                                int monthNum = 0;
                                if (dateStr.length() >= 7) {
                                    try { monthNum = Integer.parseInt(dateStr.substring(5, 7)); } catch (Exception e) { /* 무시 */ }
                                }
                                boolean showMonth = (i == 0) || (dayNum == 1);
                                views.setTextViewText(dateId, buildDateText(dayNum, monthNum, showMonth, holidayName));

                                // 다른 달 날짜는 앱 달력 탭(.other-month, opacity 0.38)과 같은
                                // 톤으로 흐리게(2026-07-22 추가) — 칸 전체(cell_container)에
                                // 알파를 줘서 날짜 숫자·근무 배지 전부 같이 흐려짐.
                                if (otherMonth) {
                                    views.setFloat(cellId, "setAlpha", 0.38f);
                                }

                                if (!shiftName.isEmpty()) {
                                    boolean continuesBand = !batchId.isEmpty() && batchId.equals(prevBatchId);
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
                                prevBatchId = batchId.isEmpty() ? null : batchId;

                                if (dateStr.equals(todayStr)) {
                                    views.setTextColor(dateId, 0xFF007AFF);
                                    // 스케줄 위젯과 같은 방식 — 날짜 숫자만이 아니라 그 날
                                    // 칸 전체에 테두리를 둘러서 표시.
                                    views.setInt(cellId, "setBackgroundResource", R.drawable.widget_today_cell_border);
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
