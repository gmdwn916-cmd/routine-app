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
import android.net.Uri;
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

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_TOGGLE.equals(intent.getAction())) {
            handleToggle(context, intent);
            return;
        }
        super.onReceive(context, intent);
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
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject it = items.optJSONObject(i);
                        if (it != null && id.equals(it.optString("id", ""))) {
                            it.put("done", newDone);
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

    private static void updateOne(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today);

        Intent openIntent = new Intent(context, MainActivity.class);
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
        boolean isDark = (context.getResources().getConfiguration().uiMode
            & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
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

        int primaryText = ContextCompat.getColor(context, R.color.widget_text_primary);
        int secondaryText = ContextCompat.getColor(context, R.color.widget_text_secondary);
        views.setTextViewText(idFor(context, "today_date"), "");
        views.setTextColor(idFor(context, "today_date"), primaryText);
        views.setTextViewText(idFor(context, "today_shift"), "");
        views.setTextColor(idFor(context, "today_shift"), secondaryText);
        views.setInt(idFor(context, "today_shift"), "setBackgroundColor", 0x00000000);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_TODAY_DATA, null);
        if (raw != null) {
            try {
                JSONObject obj = new JSONObject(raw);
                views.setTextViewText(idFor(context, "today_date"), obj.optString("dateLabel", ""));

                String shiftName = obj.optString("shiftName", "");
                String color = obj.optString("color", "");
                if (!shiftName.isEmpty()) {
                    views.setTextViewText(idFor(context, "today_shift"), shiftName);
                    if (!color.isEmpty()) {
                        try {
                            int base = Color.parseColor(color);
                            // 앱의 근무 배지(applyShiftBadgeColor)와 같은 느낌:
                            // 배경은 옅게(약 15% 불투명도), 글자는 근무색 그대로.
                            int tintedBg = (base & 0x00FFFFFF) | 0x26000000;
                            views.setInt(idFor(context, "today_shift"), "setBackgroundColor", tintedBg);
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

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
