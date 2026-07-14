package com.hyeongju.routineapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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

    private static void updateOne(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_inbox);

        Intent openIntent = new Intent(context, MainActivity.class);
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

        int primaryText = ContextCompat.getColor(context, R.color.widget_text_primary);
        int secondaryText = ContextCompat.getColor(context, R.color.widget_text_secondary);
        views.setTextViewText(idFor(context, "inbox_title"), "미배치 목록");
        views.setTextColor(idFor(context, "inbox_title"), primaryText);
        views.setTextViewText(idFor(context, "inbox_count"), "");
        views.setTextColor(idFor(context, "inbox_count"), secondaryText);

        // 배경도 글자색과 같은 순간·같은 판단(isDark)으로 직접 골라 심음 —
        // 위젯 2·3·4와 동일한 이유(런처가 실시간으로 다시 그리는 배경과, 우리가
        // push 시점에 확정해 심는 글자색이 서로 다른 시점의 테마를 따르면서
        // 어긋나는 사고 방지).
        boolean isDark = (context.getResources().getConfiguration().uiMode
            & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
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
