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
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

// N주 스케줄 위젯 — 한 줄에 한 주(7칸)씩 보여주는 요일 그리드 + 페이지(주 단위)
// 넘기기. 위젯 2(달력)와 완전히 같은 원칙 — 근무 계산·할 일 목록 판단은 전부
// JS(index.html)가 끝내고 결과만 넘겨줌(WidgetBridgePlugin.setScheduleData).
// 여기서 하는 일은 그 결과를 그리는 것과 페이지 위치 기억뿐 — 근무·할일 로직 없음.
// **2026-07-21 재설계**: 4x1~4x5로 리사이즈 가능해지면서, 위젯 크기(세로
// 줄 수)에 맞춰 몇 주를 보여줄지 동적으로 정함 — 레이아웃에 최대 5줄
// (sch_week_row_0~4, 각 7칸)을 미리 선언해두고 실제 보여줄 줄 수만 남기고
// 나머지는 GONE. JS가 넘겨주는 `weeks[]`(예전 `pages[]`를 "2주" 단위에서
// "1주" 단위로 세분화, 넉넉한 범위를 한 번에 계산해 넘김)에서 현재 위젯
// 크기에 맞는 구간만 그림.
public class ScheduleWidgetProvider extends AppWidgetProvider {
    public static final String PREFS_NAME = "widget_bridge";
    public static final String KEY_SCHEDULE_DATA = "schedule_widget_data";
    // 예전 "schedule_widget_page_index"(2주 단위 페이지 인덱스)를 "주 단위
    // 시작 인덱스"로 의미를 바꿔 이름도 같이 바꿈 — 예전 키를 그대로 쓰면
    // 이름과 실제 단위가 안 맞아 헷갈릴 위험이 있어서 새 키로 교체(구버전
    // 값이 남아있어도 새 키는 없으니 그냥 기본값(DEFAULT_WEEK_START_INDEX)
    // 으로 자연스럽게 시작함 — 별도 마이그레이션 불필요).
    public static final String KEY_WEEK_START_INDEX = "schedule_widget_week_start_index";

    private static final int MAX_TODOS_PER_CELL = 3;
    private static final int MAX_WEEKS = 5; // 레이아웃에 미리 선언해둔 최대 줄 수
    private static final int DEFAULT_VISIBLE_WEEKS = 2; // 위젯 크기를 못 읽었을 때의 기본값(예전 고정 2주와 동일)
    // JS의 SCHEDULE_WEEK_OFFSET_START(-5)와 반드시 같은 값 — weeks[] 배열의
    // 몇 번째 인덱스가 "이번 주"(오프셋 0)인지 계산하는 데 씀.
    private static final int WEEK_OFFSET_START = -5;
    // "이번 주"가 첫 줄에 오도록 — WidgetBridgePlugin.setScheduleData()가 새
    // 자료를 받을 때마다 이 값으로 되돌리기 위해 public으로 노출.
    public static final int DEFAULT_WEEK_START_INDEX = -WEEK_OFFSET_START;

    // 위젯 크기(세로 dp)로 몇 주(줄)를 보여줄지 어림잡는 데 쓰는 상수 —
    // 예전 "2주 고정"일 때의 minHeight(110dp) 기준으로 역산함: 헤더(요일
    // 이름 줄)+위아래 패딩을 대략 32dp로, 남는 공간을 2로 나누면 한 줄당
    // 대략 39dp. 정확한 값이 아니라 어림값(다른 위젯들의 estimateVisibleRows()
    // 와 같은 성격) — 틀려도 줄 수가 1~2줄 정도 어긋날 뿐 깨지지 않음.
    private static final int HEADER_AND_PADDING_DP = 32;
    private static final int WEEK_ROW_HEIGHT_DP = 39;

    private static final String ACTION_PREV = "com.hyeongju.routineapp.SCHEDULE_PREV";
    private static final String ACTION_NEXT = "com.hyeongju.routineapp.SCHEDULE_NEXT";

    // 달력 위젯과 같은 값(웹의 --holiday-color) — 자세한 이유는 그쪽 주석 참고.
    private static final int HOLIDAY_COLOR = 0xFFD9645E;

    // 완료된 할 일은 회색 취소선으로(2026-07-20 추가, 설정 탭 "완료된 항목" 위젯
    // 토글) — todos/allTodos가 이제 {text, done} 객체라 그 done을 보고 취소선
    // span만 얹음. 텍스트 색은 XML 기본값(widget_text_secondary)을 그대로 두고
    // (완료든 아니든 이 위젯의 할 일 줄은 원래도 회색 계열) 취소선만 추가.
    private static CharSequence buildTodoText(String text, boolean done) {
        if (!done) return text;
        SpannableString ss = new SpannableString(text);
        ss.setSpan(new StrikethroughSpan(), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }

    // showMonth가 true면 "7/21"처럼 월을 같이 붙임(2026-07-21 추가) — 화면에
    // 보이는 첫 줄의 첫 칸(그 줄이 화면에 처음 보이는 날이라 어느 달인지
    // 바로 안 보일 수 있음)과, 실제로 달이 넘어가는 칸(dayNum이 1로 돌아오는
    // 칸) 둘 다 이 조건에 해당 — 보이는 범위가 두 달에 걸치면(예: 7/21~8/3)
    // 두 군데 다 월이 붙어서 보임("2개 다 표시" 요청). 나머지 칸은 그대로
    // 날짜 숫자만.
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
            int idx = prefs.getInt(KEY_WEEK_START_INDEX, DEFAULT_WEEK_START_INDEX);
            int totalWeeks = getWeekCount(context);
            // 여러 인스턴스가 있으면 이 위젯들은 시작 인덱스를 공유하므로
            // (예전 페이지 인덱스도 마찬가지), 몇 주씩 넘길지는 그중 첫
            // 인스턴스의 현재 크기를 기준으로 정함 — 사실상 거의 항상
            // 인스턴스가 하나뿐이라 실사용에서 문제되지 않음.
            int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, ScheduleWidgetProvider.class));
            int visibleWeeks = ids.length > 0 ? estimateVisibleWeeks(context, ids[0]) : DEFAULT_VISIBLE_WEEKS;
            int maxIdx = Math.max(0, totalWeeks - visibleWeeks);
            idx = ACTION_PREV.equals(action) ? Math.max(0, idx - visibleWeeks) : Math.min(maxIdx, idx + visibleWeeks);
            prefs.edit().putInt(KEY_WEEK_START_INDEX, idx).apply();
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

    // 위젯 크기가 바뀌면(4x1~4x5 리사이즈, 2026-07-21 추가) 몇 주를 보여줄지도
    // 다시 계산해야 함(다른 위젯들의 onAppWidgetOptionsChanged와 같은 이유).
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
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

    private static int getWeekCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_SCHEDULE_DATA, null);
        if (raw == null) return 1;
        try {
            JSONArray weeks = new JSONObject(raw).optJSONArray("weeks");
            return weeks != null ? Math.max(1, weeks.length()) : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    // 위젯 크기(세로 dp)로 몇 주(줄)를 보여줄지 어림잡음 — 다른 위젯들의
    // estimateVisibleRows()와 같은 성격의 어림 계산(정확하지 않아도 됨,
    // 틀리면 그냥 줄 수가 조금 어긋날 뿐).
    private static int estimateVisibleWeeks(Context context, int appWidgetId) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return DEFAULT_VISIBLE_WEEKS;
        try {
            Bundle opts = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId);
            int minHeightDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
            if (minHeightDp <= 0) return DEFAULT_VISIBLE_WEEKS;
            int usableDp = minHeightDp - HEADER_AND_PADDING_DP;
            int weeks = usableDp / WEEK_ROW_HEIGHT_DP;
            if (weeks < 1) weeks = 1;
            if (weeks > MAX_WEEKS) weeks = MAX_WEEKS;
            return weeks;
        } catch (Exception e) {
            return DEFAULT_VISIBLE_WEEKS;
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

    // 위젯이 지금 보여주고 있는 첫 줄(주)의 첫날을 기준으로 "그 달"(YYYY-MM)을
    // 뽑아서 앱을 열 때 달력 탭이 그 달로 바로 이동하게 함 — 달력 위젯의
    // currentDisplayedMonth()와 같은 방식(이미 JS가 계산해서 저장해둔 date
    // 문자열에서 잘라 쓰는 것뿐, 근무 계산 아님).
    private static String currentDisplayedMonth(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_SCHEDULE_DATA, null);
        if (raw == null) return null;
        try {
            JSONArray weeks = new JSONObject(raw).optJSONArray("weeks");
            if (weeks == null || weeks.length() == 0) return null;
            int idx = prefs.getInt(KEY_WEEK_START_INDEX, DEFAULT_WEEK_START_INDEX);
            if (idx < 0) idx = 0;
            if (idx > weeks.length() - 1) idx = weeks.length() - 1;
            JSONObject weekObj = weeks.optJSONObject(idx);
            if (weekObj == null) return null;
            JSONArray days = weekObj.optJSONArray("days");
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

        int visibleWeeks = estimateVisibleWeeks(context, appWidgetId);

        // 위젯 크기(리사이즈)에 맞는 줄만 보이게, 나머지는 GONE(2026-07-21
        // 추가) — GONE인 형제는 부모 LinearLayout의 weight 배분에서 자동으로
        // 빠지므로, 보이는 줄들이 남는 세로 공간을 알아서 나눠 채움.
        for (int r = 0; r < MAX_WEEKS; r++) {
            views.setViewVisibility(idFor(context, "sch_week_row_" + r), r < visibleWeeks ? View.VISIBLE : View.GONE);
        }

        // 자료가 없거나 깨져 있어도 안전하게 빈 상태로 시작(최대 35칸 전부)
        for (int i = 0; i < 7; i++) {
            views.setTextViewText(idFor(context, "sch_header_" + i), "");
            views.setTextColor(idFor(context, "sch_header_" + i), secondaryText);
        }
        for (int i = 0; i < MAX_WEEKS * 7; i++) {
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

                JSONArray weeks = obj.optJSONArray("weeks");
                if (weeks != null && weeks.length() > 0) {
                    int startIdx = prefs.getInt(KEY_WEEK_START_INDEX, DEFAULT_WEEK_START_INDEX);
                    int maxStart = Math.max(0, weeks.length() - visibleWeeks);
                    if (startIdx < 0) startIdx = 0;
                    if (startIdx > maxStart) startIdx = maxStart;

                    // 진짜 근무 일괄 수정(같은 batchId)으로 이어진 날짜만 글자를 첫
                    // 칸에만 쓰고 나머지는 색만 이어붙임(2026-07-18 추가 — 예전엔
                    // 이런 구분이 아예 없어서 "괌"처럼 여러 날을 한 번에 바꾼 근무가
                    // 모든 칸에 그대로 반복 표시되는 문제가 있었음. 달력 위젯·앱
                    // 화면의 appendShiftIndicator와 같은 기준으로 통일). **줄(주)이
                    // 바뀔 때 강제로 초기화하지 않음(2026-07-18 재수정)** — weeks
                    // 배열은 항상 날짜순으로 이어져 있으므로(주 사이도 끊김 없음),
                    // 배열의 바로 앞 칸이 곧 "어제"이므로 줄 경계와 무관하게 그대로
                    // 이어짐 — 줄마다 초기화하면 그 경계에서만 이름이 다시 튀어나오는
                    // 버그가 있었음(앱 화면은 이런 "줄" 개념 자체가 없어서 이 버그가
                    // 없었음).
                    String prevBatchId = null;
                    for (int r = 0; r < visibleWeeks; r++) {
                        JSONObject weekObj = weeks.optJSONObject(startIdx + r);
                        JSONArray days = weekObj != null ? weekObj.optJSONArray("days") : null;
                        for (int c = 0; c < 7; c++) {
                            int i = r * 7 + c;
                            Object dayObj = days != null ? days.opt(c) : null;
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
                            // 날짜 칸 팝업(DayQuickViewActivity)용 전체 목록(2026-07-19
                            // 추가) — 위 todos는 위젯 칸 안에 실제로 그릴 수 있는 만큼만
                            // (최대 3줄)이라, 팝업에도 그걸 그대로 재사용하면 그 이상은
                            // 절대 안 보이는 문제가 있었음("위젯에 표시된 것만 보여준다"는
                            // 신고). 팝업은 화면이 넓어서 다 보여줄 수 있으므로 JS가 같이
                            // 보내주는 전체 목록을 씀 — 없으면(구버전 데이터 등) todos로
                            // 대체.
                            JSONArray allTodos = day.optJSONArray("allTodos");
                            if (allTodos == null) allTodos = todos;

                            int dateId = idFor(context, "sch_date_" + i);
                            int shiftId = idFor(context, "sch_shift_" + i);
                            int cellId = idFor(context, "sch_cell_" + i);

                            int monthNum = 0;
                            if (dateStr.length() >= 7) {
                                try { monthNum = Integer.parseInt(dateStr.substring(5, 7)); } catch (Exception e) { /* 무시 */ }
                            }
                            // 화면에 보이는 첫 줄의 첫 칸(r==0 && c==0, 그 전 날짜가
                            // 안 보여서 몇 월인지 바로 안 드러날 수 있음) 또는 달이
                            // 실제로 넘어가는 칸(dayNum==1)일 때만 월을 붙임.
                            boolean showMonth = (r == 0 && c == 0) || (dayNum == 1);
                            views.setTextViewText(dateId, buildDateText(dayNum, monthNum, showMonth, holidayName));
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
                                    // {text, done} 객체(2026-07-20 추가)와 예전 순수 문자열
                                    // 형식을 둘 다 안전하게 처리 — optJSONObject가 null이면
                                    // (구버전 데이터 등) optString으로 바로 문자열을 읽음.
                                    JSONObject todoObj = todos.optJSONObject(t);
                                    String text = todoObj != null ? todoObj.optString("text", "") : todos.optString(t, "");
                                    boolean done = todoObj != null && todoObj.optBoolean("done", false);
                                    views.setTextViewText(idFor(context, "sch_todo_" + i + "_" + t), buildTodoText(text, done));
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
                                if (allTodos != null) dayIntent.putExtra(DayQuickViewActivity.EXTRA_TODOS, allTodos.toString());
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
            } catch (Exception e) {
                // 저장된 데이터가 깨져 있으면 위에서 이미 비워둔 상태로 둠
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
