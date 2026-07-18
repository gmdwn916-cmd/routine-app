package com.hyeongju.routineapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

// 미배치(인박스) 목록을 그대로 보여주는 읽기 전용 위젯(위젯 5). 근무·날짜
// 계산이 아예 없어서 위젯 2·3·4보다 훨씬 단순함 — JS(index.html)가 넘겨준
// 목록(count, items)을 저장하고 그리기만 함. 목록(ListView)을 채우는 건
// InboxWidgetService/InboxRemoteViewsFactory가 맡고, 여기(Provider)는 헤더
// (제목+개수) 그리기만 담당. 체크·추가 같은 조작이 없는 순수 조회용이라
// 위젯 4(오늘 할일)와 달리 임시 보관함(pending 처리)도 필요 없음 — 항목을
// 탭하면 그냥 앱만 열림(어떤 항목인지는 구분 안 함, PendingIntentTemplate에
// 빈 fillInIntent만 붙여서 모든 줄이 똑같이 앱을 엶).
public class InboxWidgetProvider extends AppWidgetProvider {
    public static final String PREFS_NAME = "widget_bridge";
    public static final String KEY_INBOX_DATA = "inbox_widget_data";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateOne(context, appWidgetManager, id);
        }
    }

    // 휴대폰 시스템 다크/라이트 설정이 바뀌면 앱을 안 열어도 위젯을 새로
    // 그림(2026-07-18 추가) — WidgetThemeHelper.isDarkMode()가 매번 다시
    // 판단하므로 refreshAll()만 다시 부르면 됨(목록도 같이 새로고침됨).
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
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, InboxWidgetProvider.class));
        for (int id : ids) {
            updateOne(context, mgr, id);
        }
        if (ids.length > 0) {
            mgr.notifyAppWidgetViewDataChanged(ids, idFor(context, "inbox_list"));
        }
    }

    private static int idFor(Context context, String name) {
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }

    // **버그(2026-07-18, 도입 당일 되돌림)**: 오늘 위젯과 똑같은 이유로 시도했던
    // widget_inbox_v31.xml + setViewLayoutHeight 방식(자세한 경위는
    // TodayWidgetProvider.updateOne()의 2026-07-18 기록 참고)이 항목이 많을
    // 때 목록 스크롤이 안 되고 그 아래 있던 inbox_list_filler·"+" 버튼까지
    // 위젯 화면 밖으로 밀려나는 회귀를 일으켜서 되돌림 — widget_inbox_v31.xml
    // 삭제, inbox_list는 다시 weight=1로 통일. 같은 방식으로 재시도하지 말 것.
    private static void updateOne(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_inbox);

        Intent openIntent = new Intent(context, MainActivity.class);
        // 위젯마다 다른 action을 붙여서 서로 다른 PendingIntent로 구분되게 함(달력
        // 위젯 항목의 2026-07-15 수정 참고 — 안 붙이면 다른 위젯들과 하나의
        // PendingIntent로 뭉쳐져서 아무 위젯이나 눌러도 마지막에 갱신된 위젯의
        // 목적지로만 열리는 버그가 있었음. 이 위젯은 매번 이 목적지 하나뿐이라
        // 원래도 문제가 덜 드러났지만, 다른 위젯들이 전부 "미배치"로 끌려가던
        // 원인이 바로 이 위젯이 매번 갱신 순서상 마지막이라 그 목적지로 다른
        // 위젯들의 PendingIntent까지 덮어썼기 때문이었음).
        openIntent.setAction("com.hyeongju.routineapp.OPEN_APP_INBOX");
        openIntent.putExtra(MainActivity.EXTRA_WIDGET_NAV, "inbox");
        PendingIntent openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(idFor(context, "widget_inbox_root"), openPending);

        // 목록(ListView)을 채우는 건 InboxWidgetService/InboxRemoteViewsFactory가
        // 담당 — 여기서는 그 서비스를 가리키는 어댑터만 연결(위젯 4와 같은 패턴,
        // appWidgetId를 데이터에 실어 유일하게 만들어야 위젯 인스턴스마다 올바르게
        // 구분됨).
        Intent adapterIntent = new Intent(context, InboxWidgetService.class);
        adapterIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        adapterIntent.setData(Uri.parse(adapterIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(idFor(context, "inbox_list"), adapterIntent);
        views.setEmptyView(idFor(context, "inbox_list"), idFor(context, "inbox_empty"));

        // 읽기 전용이라 모든 줄이 항상 같은 동작(앱 열기)만 하면 되지만, 안드로이드
        // 컬렉션 위젯(ListView)은 줄마다 (내용이 비어있더라도) fillInIntent가
        // 있어야 탭에 반응함(없으면 스크롤만 되고 탭은 씹힘) — 그래서 앱 열기용
        // PendingIntent를 템플릿으로 하나 더 만들어 씀. 위젯 4의 체크 토글
        // 템플릿과 같은 이유로 FLAG_MUTABLE로 만듦(내용이 비어있는
        // fillInIntent라도 시스템이 병합을 시도하므로, 다른 위젯들의 단순 클릭과
        // 달리 이 템플릿 전용 PendingIntent만 FLAG_IMMUTABLE을 안 씀).
        PendingIntent openPendingForList = PendingIntent.getActivity(
            context, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
        views.setPendingIntentTemplate(idFor(context, "inbox_list"), openPendingForList);

        // 맨 밑의 큼지막한 + 버튼(2026-07-16 추가) — 목록은 안 보여주고 이
        // 버튼만 눌러서 QuickAddActivity(위젯 1과 완전히 같은 입력창, 새로
        // 만들지 않고 그대로 재사용)를 띄움 — 거기서 쓴 글자는 위젯 1과 똑같은
        // "임시 우편함"(pending_inbox_items)에 쌓였다가 앱이 열릴 때
        // syncWidgetInboxItems()가 미배치로 옮김. requestCode(2)가 이 Provider
        // 안의 다른 PendingIntent(0, 1)와도, 위젯 1의 QuickAddActivity 인텐트
        // (requestCode 0)와도 겹치지 않게 다르고, 그래도 만약을 대비해 action도
        // 따로 붙여 완전히 구분함(이 프로젝트에서 위젯 인텐트 충돌을 겪고
        // 배운 습관). FLAG_ACTIVITY_NEW_TASK|MULTIPLE_TASK도 위젯 1과 동일하게
        // 줌 — QuickAddActivity의 taskAffinity=""와 같이 써야 앱이 이미 켜져
        // 있어도 입력창만 별도 작업으로 뜨고 앱 화면까지 같이 안 끌려나옴.
        Intent addIntent = new Intent(context, QuickAddActivity.class);
        addIntent.setAction("com.hyeongju.routineapp.OPEN_QUICK_ADD_FROM_INBOX_WIDGET");
        addIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        PendingIntent addPending = PendingIntent.getActivity(
            context, 2, addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(idFor(context, "inbox_add_button"), addPending);

        int primaryText = WidgetThemeHelper.primaryTextColor(context);
        int secondaryText = ContextCompat.getColor(context, R.color.widget_text_secondary);
        views.setTextViewText(idFor(context, "inbox_title"), "미배치 목록");
        views.setTextColor(idFor(context, "inbox_title"), primaryText);
        views.setTextViewText(idFor(context, "inbox_count"), "");
        views.setTextColor(idFor(context, "inbox_count"), secondaryText);

        // 배경도 글자색과 같은 순간·같은 판단(isDark)으로 직접 골라 심음 —
        // 위젯 2·3·4와 동일한 이유(런처가 실시간으로 다시 그리는 배경과, 우리가
        // push 시점에 확정해 심는 글자색이 서로 다른 시점의 테마를 따르면서
        // 어긋나는 사고 방지).
        boolean isDark = WidgetThemeHelper.isDarkMode(context);
        views.setInt(idFor(context, "widget_inbox_root"), "setBackgroundResource",
            isDark ? R.drawable.widget_background_dark : R.drawable.widget_background_light);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_INBOX_DATA, null);
        if (raw != null) {
            try {
                JSONObject obj = new JSONObject(raw);
                int count = obj.optInt("count", 0);
                views.setTextViewText(idFor(context, "inbox_count"), count > 0 ? String.valueOf(count) + "개" : "");
            } catch (Exception e) {
                // 데이터가 깨져 있으면 위에서 이미 비워둔 빈 헤더로 둠
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
