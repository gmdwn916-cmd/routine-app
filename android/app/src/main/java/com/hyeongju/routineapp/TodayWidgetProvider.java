package com.hyeongju.routineapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

// 오늘의 근무 + 반복 할일/한 번짜리 할 일을 스크롤 가능한 목록으로 보여주고,
// 위젯 안에서 항목을 탭하면 그 자리에서 완료 체크까지 되는 위젯(위젯 4).
// 위젯 2·3과 달리 "목록"이라 안드로이드의 컬렉션 위젯 방식(RemoteViewsService +
// 어댑터)을 씀 — 목록을 실제로 채우는 일은 TodayWidgetService/
// TodayRemoteViewsFactory가 맡고, 여기(Provider)는 헤더(날짜·근무) 그리기와
// 체크 눌렀을 때 처리만 담당.
// 근무·할일 계산은 전부 JS(index.html)가 끝내서 넘겨준 결과(오늘 날짜, 근무
// 이름, 항목 목록과 완료 여부)를 그대로 저장/표시만 함 — 위젯 2·3과 같은 원칙.
// 이 위젯만 유일하게 "쓰기"(체크)가 있음 — 자세한 동기화 방식은 handleToggle 참고.
public class TodayWidgetProvider extends AppWidgetProvider {
    public static final String PREFS_NAME = "widget_bridge";
    public static final String KEY_TODAY_DATA = "today_widget_data";
    public static final String KEY_PENDING_TOGGLES = "today_widget_pending_toggles";

    private static final String ACTION_TOGGLE = "com.hyeongju.routineapp.TODAY_TOGGLE";
    static final String EXTRA_ITEM_ID = "item_id";
    static final String EXTRA_ITEM_TYPE = "item_type";
    static final String EXTRA_NEW_DONE = "new_done";
    // 목록 줄에서 체크칸이 아닌 나머지 부분을 눌렀을 때 씀(2026-07-16) — 같은
    // 신호 틀(PendingIntentTemplate) 하나를 공유하는 컬렉션 위젯 특성상 목적지가
    // 다른 별도의 클릭(액티비티를 여는 것)을 만들 수 없어서, 똑같이 이 방송
    // (ACTION_TOGGLE)을 타되 이 표시가 있으면 체크 대신 앱을 열도록 분기함.
    static final String EXTRA_OPEN_APP = "open_app";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_TOGGLE.equals(action)) {
            if (intent.getBooleanExtra(EXTRA_OPEN_APP, false)) {
                openAppToToday(context);
            } else {
                handleToggle(context, intent);
            }
            return;
        }
        // 휴대폰 시스템 다크/라이트 설정이 바뀌면 앱을 안 열어도 위젯을 새로
        // 그림(2026-07-18 추가) — WidgetThemeHelper.isDarkMode()가 매번 다시
        // 판단하므로 refreshAll()만 다시 부르면 됨(목록도 같이 새로고침됨).
        if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
            refreshAll(context);
            return;
        }
        super.onReceive(context, intent);
    }

    // 체크칸 없는 줄 영역(빈 공백 포함)을 눌렀을 때 오늘 탭으로 앱을 엶(위젯
    // 헤더를 누르는 것과 같은 목적지). **처음엔 여기서 바로 context.startActivity()를
    // 불렀는데, 화면이 잘 안 열리는 문제가 있었음(2026-07-18 발견)** — 목록
    // 항목은 구조상 방송(브로드캐스트)을 한 번 거쳐야만 탭에 반응하는데, 그
    // 방송을 받은 코드 안에서 곧바로 화면을 여는 방식은 안드로이드가 배경
    // 실행 제한 정책으로 종종 막아버려서 기기·상황에 따라 됐다 안 됐다 하는
    // 문제가 있었음(위젯 헤더처럼 시스템이 직접 화면을 여는 PendingIntent는
    // 이 제한을 안 받아서 항상 잘 열림). 고침: 여기서도 화면을 직접 열지
    // 않고, 위젯 헤더와 똑같은 방식(PendingIntent.getActivity)으로 미리
    // "화면 열기 티켓"을 만들어서 그걸 실행(send)하는 방식으로 바꿈 — 이
    // 티켓은 안드로이드가 원래부터 신뢰하는 방식이라 배경 실행 제한을 안 받음.
    private void openAppToToday(Context context) {
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setAction("com.hyeongju.routineapp.OPEN_APP_TODAY_FROM_LIST");
        openIntent.putExtra(MainActivity.EXTRA_WIDGET_NAV, "today");
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openPending = PendingIntent.getActivity(
            context, 3, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        try {
            openPending.send();
        } catch (PendingIntent.CanceledException e) {
            // 무시 — 이번 한 번 안 열려도 다음 탭에서 다시 시도됨
        }
    }

    // 위젯 목록의 한 줄을 탭했을 때: ① 저장해둔 오늘 데이터 안의 그 항목 done
    // 값을 바로 바꿔서 위젯이 즉시 체크된 걸로 보이게 하고(낙관적 갱신),
    // ② "이 항목을 이 상태로 만들어라"(토글이 아니라 최종 상태)를 임시 보관함에
    // 쌓아서 앱이 열릴 때 진짜 데이터(done_log/ev.done)에 반영하게 함 — 최종
    // 상태를 보내는 방식이라 같은 요청이 중복 반영돼도 결과가 달라지지 않음.
    private void handleToggle(Context context, Intent intent) {
        String id = intent.getStringExtra(EXTRA_ITEM_ID);
        String type = intent.getStringExtra(EXTRA_ITEM_TYPE);
        boolean newDone = intent.getBooleanExtra(EXTRA_NEW_DONE, false);
        if (id == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String dateStr = "";
        String raw = prefs.getString(KEY_TODAY_DATA, null);
        if (raw != null) {
            try {
                JSONObject obj = new JSONObject(raw);
                dateStr = obj.optString("date", "");
                JSONArray items = obj.optJSONArray("items");
                if (items != null) {
                    // 체크한 항목은 목록에서 바로 사라지게 함(완료 표시만 하고 계속
                    // 보여주던 이전 방식에서 바뀜) — payload 자체가 "미완료 항목만"
                    // 담고 있으므로, 완료로 바뀌는 항목은 그 자리에서 통째로 지움.
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject it = items.optJSONObject(i);
                        if (it != null && id.equals(it.optString("id", ""))) {
                            if (newDone) {
                                items.remove(i);
                            } else {
                                it.put("done", false);
                            }
                            break;
                        }
                    }
                }
                prefs.edit().putString(KEY_TODAY_DATA, obj.toString()).apply();
            } catch (Exception e) {
                // 무시 — 다음에 앱이 새 데이터를 보내면 다시 정상화됨
            }
        }

        try {
            JSONArray pending = new JSONArray(prefs.getString(KEY_PENDING_TOGGLES, "[]"));
            JSONObject t = new JSONObject();
            t.put("id", id);
            t.put("date", dateStr);
            t.put("type", type == null ? "once" : type);
            t.put("done", newDone);
            pending.put(t);
            prefs.edit().putString(KEY_PENDING_TOGGLES, pending.toString()).apply();
        } catch (Exception e) {
            // 무시 — 이 체크 하나는 앱에 안 넘어갈 수 있지만 다음 체크는 정상 동작
        }

        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, TodayWidgetProvider.class));
        if (ids.length > 0) {
            mgr.notifyAppWidgetViewDataChanged(ids, idFor(context, "today_list"));
        }
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
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, TodayWidgetProvider.class));
        for (int id : ids) {
            updateOne(context, mgr, id);
        }
        if (ids.length > 0) {
            mgr.notifyAppWidgetViewDataChanged(ids, idFor(context, "today_list"));
        }
    }

    private static int idFor(Context context, String name) {
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }

    // 목록 줄 하나의 대략적인 높이(dp) — widget_today_item.xml의
    // paddingTop/Bottom(6+6=12dp) + 내용물(체크 아이콘 20dp) 기준. 실측이
    // 아니라 어림값이라, 살짝 더 크게 잡기보다 살짝 작게 잡음(더 크게 잡으면
    // 목록 안쪽에 빈 여백이 남아 이번에 고치려는 것과 같은 종류의 "눌러도
    // 반응 없는 빈틈"이 다시 생기지만, 작게 잡으면 마지막 한 줄만 살짝 스크롤이
    // 필요해지는 정도라 훨씬 무해함).
    private static final int TODAY_ROW_HEIGHT_DP = 30;

    private static void updateOne(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // 목록 안쪽 빈 여백을 눌러도 반응 없던 문제(2026-07-18) 수정 —
        // API 31(안드로이드 12) 이상에서만 setViewLayoutHeight로 목록
        // (today_list) 높이를 실제 항목 수만큼만 정확히 지정하고, 그 아래
        // 남는 진짜 여백은 별도 뷰(today_list_filler)가 차지해서 항상 눌리게
        // 함 — 이 계산을 지원 못 하는 그 미만 기기는 예전 그대로인
        // widget_today.xml(today_list가 weight=1로 남은 공간을 전부 차지,
        // 그 안 빈 여백은 예전처럼 안 눌릴 수 있음)을 그대로 씀.
        boolean useV31Layout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
        RemoteViews views = new RemoteViews(context.getPackageName(),
            useV31Layout ? R.layout.widget_today_v31 : R.layout.widget_today);

        Intent openIntent = new Intent(context, MainActivity.class);
        // 위젯마다 다른 action을 붙여서 서로 다른 PendingIntent로 구분되게 함(달력
        // 위젯 항목의 2026-07-15 수정 참고 — 안 붙이면 다른 위젯들과 하나의
        // PendingIntent로 뭉쳐져서 아무 위젯이나 눌러도 마지막에 갱신된 위젯의
        // 목적지로만 열리는 버그가 있었음).
        openIntent.setAction("com.hyeongju.routineapp.OPEN_APP_TODAY");
        openIntent.putExtra(MainActivity.EXTRA_WIDGET_NAV, "today");
        PendingIntent openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(idFor(context, "widget_today_root"), openPending);

        // 배경을 XML의 @drawable/widget_background로 그냥 두면 홈 화면 런처가
        // "그 순간 자신의" 다크/라이트 상태로 실시간 재해석해서 그리는데, 글자색은
        // 우리 앱이 마지막으로 push한 순간의 다크/라이트 상태로 이미 확정돼 심어짐 —
        // 그 사이 시스템 다크/라이트가 바뀌면 배경만 새 상태로 바뀌고 글자색은
        // 예전 상태로 남아 어긋날 수 있음(예: 라이트로 바뀐 흰 배경 위에 다크 모드
        // 때 심어둔 흰 글자가 그대로 남아 안 보이는 사고). 배경도 글자색과 완전히
        // 같은 순간·같은 판단(isDark)으로 우리가 직접 골라 심어서 항상 맞게 함.
        boolean isDark = WidgetThemeHelper.isDarkMode(context);
        views.setInt(idFor(context, "widget_today_root"), "setBackgroundResource",
            isDark ? R.drawable.widget_background_dark : R.drawable.widget_background_light);

        // 목록(ListView)을 채우는 건 TodayWidgetService/TodayRemoteViewsFactory가
        // 담당 — 여기서는 그 서비스를 가리키는 어댑터만 연결. appWidgetId를 인텐트
        // 데이터에 실어 유일하게 만들어야(setData) 안드로이드가 위젯 인스턴스마다
        // 다른 어댑터로 올바르게 구분함(공식 문서 권장 패턴).
        Intent adapterIntent = new Intent(context, TodayWidgetService.class);
        adapterIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        adapterIntent.setData(Uri.parse(adapterIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(idFor(context, "today_list"), adapterIntent);
        views.setEmptyView(idFor(context, "today_list"), idFor(context, "today_empty"));

        // 목록의 각 줄이 탭됐을 때 공통으로 쓸 신호 틀(템플릿) — 실제로 어떤
        // 항목인지는 TodayRemoteViewsFactory가 각 줄에 붙여둔 정보(fillInIntent)와
        // 합쳐져서 전달됨. 이 틀은 반드시 내용을 채울 수 있는 상태(FLAG_MUTABLE)여야
        // 그 정보가 실제로 합쳐짐 — 다른 위젯의 눌림(FLAG_IMMUTABLE)과 다른 부분.
        Intent toggleIntent = new Intent(context, TodayWidgetProvider.class);
        toggleIntent.setAction(ACTION_TOGGLE);
        PendingIntent togglePending = PendingIntent.getBroadcast(
            context, 0, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
        views.setPendingIntentTemplate(idFor(context, "today_list"), togglePending);

        int primaryText = WidgetThemeHelper.primaryTextColor(context);
        int secondaryText = ContextCompat.getColor(context, R.color.widget_text_secondary);
        views.setTextViewText(idFor(context, "today_date"), "");
        views.setTextColor(idFor(context, "today_date"), primaryText);
        views.setTextViewText(idFor(context, "today_shift"), "");
        views.setTextColor(idFor(context, "today_shift"), secondaryText);
        views.setViewVisibility(idFor(context, "today_shift_bg"), View.GONE);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_TODAY_DATA, null);
        // 아래 + 버튼(TodayQuickAddActivity로 넘길 오늘 날짜)용으로 같이 뽑아둠 —
        // 데이터가 아직 없으면(위젯을 처음 추가해서 아직 한 번도 push 안 됐을
        // 때) 빈 채로 두고, 버튼을 누른 순간에만 기기의 오늘 날짜로 대신 채움
        // (아래 참고, 이건 근무 계산이 아니라 단순 오늘 날짜라 네이티브에서
        // 계산해도 되는 예외).
        String todayDate = "";
        // today_list_filler 계산용(2026-07-18) — 목록에 실제로 보일 항목
        // 개수를 여기서도 한 번 더 셈(TodayRemoteViewsFactory.onDataSetChanged와
        // 같은 기준: done이 아닌 것만). 위 TODAY_ROW_HEIGHT_DP와 곱해서 목록
        // 높이를 정확히 지정하는 데 씀.
        int visibleItemCount = 0;
        if (raw != null) {
            try {
                JSONObject obj = new JSONObject(raw);
                views.setTextViewText(idFor(context, "today_date"), obj.optString("dateLabel", ""));
                todayDate = obj.optString("date", "");

                JSONArray itemsArr = obj.optJSONArray("items");
                if (itemsArr != null) {
                    for (int i = 0; i < itemsArr.length(); i++) {
                        JSONObject it = itemsArr.optJSONObject(i);
                        if (it != null && !it.optBoolean("done", false)) visibleItemCount++;
                    }
                }

                String shiftName = obj.optString("shiftName", "");
                String color = obj.optString("color", "");
                if (!shiftName.isEmpty()) {
                    views.setTextViewText(idFor(context, "today_shift"), shiftName);
                    if (!color.isEmpty()) {
                        try {
                            int base = Color.parseColor(color);
                            // 앱의 근무 배지(applyShiftBadgeColor)와 같은 느낌:
                            // 배경은 옅게(약 15% 불투명도), 글자는 근무색 그대로.
                            // 모서리를 둥글게 유지하려고 setBackgroundColor(각지게
                            // 바뀌어버림) 대신, 둥근 밑그림(today_shift_bg)을 보이게
                            // 하고 그 위에 setColorFilter로 색만 입힘(2026-07-16).
                            int tintedBg = (base & 0x00FFFFFF) | 0x26000000;
                            int badgeBaseRes = isDark ? R.drawable.widget_badge_base_dark : R.drawable.widget_badge_base_light;
                            views.setImageViewResource(idFor(context, "today_shift_bg"), badgeBaseRes);
                            views.setInt(idFor(context, "today_shift_bg"), "setColorFilter", tintedBg);
                            views.setViewVisibility(idFor(context, "today_shift_bg"), View.VISIBLE);
                            views.setTextColor(idFor(context, "today_shift"), base | 0xFF000000);
                        } catch (IllegalArgumentException e) {
                            // 색상 파싱 실패 시 배경 없이 글자만 표시
                        }
                    }
                }

            } catch (Exception e) {
                // 데이터가 깨져 있으면 위에서 이미 비워둔 빈 헤더로 둠
            }
        }

        // today_list 높이를 실제 항목 수만큼만 지정하고, 남는 진짜 여백은
        // today_list_filler가 차지해서 항상 눌리게 함(2026-07-18) — v31
        // 레이아웃에서만 존재하는 뷰라 이 분기 안에서만 다룸(예전 레이아웃에는
        // today_list_filler 자체가 없어서 손댈 필요도 없음).
        if (useV31Layout) {
            // TodayRemoteViewsFactory의 FILLER_COUNT(1)과 맞춰 목록 자체
            // 안에도 빈 줄 하나가 더 있음 — 그만큼 높이에도 포함.
            int rowsForHeight = visibleItemCount + 1;
            views.setViewLayoutHeight(idFor(context, "today_list"),
                rowsForHeight * TODAY_ROW_HEIGHT_DP, TypedValue.COMPLEX_UNIT_DIP);
            views.setOnClickPendingIntent(idFor(context, "today_list_filler"), openPending);
        }

        // 맨 밑의 큼지막한 + 버튼(2026-07-16 추가, 같은 날 재수정) — 처음엔
        // 스케줄 위젯의 날짜 팝업(DayQuickViewActivity)을 오늘 날짜로 재사용해서
        // 날짜·근무·할일 목록까지 같이 보여줬는데, 사용자가 "다른거 보여주지
        // 말고 할일추가 위젯처럼만 띄워줘"로 요청해서 TodayQuickAddActivity
        // (QuickAddActivity와 똑같이 입력칸 하나뿐인 화면)로 바꿈 — 저장 위치만
        // 다름: 미배치가 아니라 그 날짜(오늘)에 바로 배치된 할 일로 들어감
        // (pending_dated_items → syncWidgetDatedItems() 경로는 그대로 재사용).
        // requestCode(2)와 action에 "_FROM_TODAY_WIDGET" 접미사를 붙여서 스케줄
        // 위젯이 같은 날짜로 만드는 DayQuickViewActivity 인텐트와 절대 안
        // 겹치게 함(다른 컴포넌트라 원래도 안 겹치지만, 이 프로젝트의 습관대로
        // 이중 안전장치). 위젯 1(QuickAddWidgetProvider)과 같은 이유로
        // NEW_TASK|MULTIPLE_TASK 플래그도 줌 — TodayQuickAddActivity의
        // taskAffinity=""와 짝을 이뤄야 앱이 이미 켜져 있어도 입력창만 별도
        // 작업으로 뜨고 앱 화면까지 같이 안 끌려나옴.
        if (todayDate.isEmpty()) {
            todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(new java.util.Date());
        }
        Intent addIntent = new Intent(context, TodayQuickAddActivity.class);
        addIntent.setAction("com.hyeongju.routineapp.OPEN_DAY_" + todayDate + "_FROM_TODAY_WIDGET");
        addIntent.putExtra(TodayQuickAddActivity.EXTRA_DATE, todayDate);
        addIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        PendingIntent addPending = PendingIntent.getActivity(
            context, 2, addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(idFor(context, "today_add_button"), addPending);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
