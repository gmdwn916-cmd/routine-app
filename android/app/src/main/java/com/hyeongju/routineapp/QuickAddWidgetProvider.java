package com.hyeongju.routineapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class QuickAddWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateOne(context, appWidgetManager, appWidgetId);
        }
    }

    // 휴대폰 시스템 다크/라이트 설정이 바뀌면 앱을 안 열어도 위젯을 새로
    // 그림(2026-07-18 추가) — WidgetThemeHelper.isDarkMode()가 매번 다시
    // 판단하므로 refreshAll()만 다시 부르면 됨.
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction())) {
            refreshAll(context);
            return;
        }
        super.onReceive(context, intent);
    }

    // 이 위젯은 앱과 주고받는 데이터가 없어서 다른 위젯들처럼 setXXXData를 통해
    // 다시 그려질 계기가 없었음 — 그래서 라이트/다크 설정을 바꿔도 앱을 다시
    // 열어도 최초 배치 때 색이 그대로 남는 버그가 있었음. WidgetBridgePlugin이
    // 앱이 열릴 때마다(pushAllWidgets) 이 함수를 불러서 다른 위젯들과 같은
    // 시점에 색을 다시 판단하게 함.
    public static void refreshAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, QuickAddWidgetProvider.class));
        for (int id : ids) {
            updateOne(context, mgr, id);
        }
    }

    private static void updateOne(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quick_add);
        Intent intent = new Intent(context, QuickAddActivity.class);
        // NEW_TASK + MULTIPLE_TASK: QuickAddActivity의 taskAffinity=""와 같이 써야
        // 앱이 이미 켜져 있어도(백그라운드/최근앱) 그 작업(task)을 앞으로 안 끌어오고,
        // 입력창만 별도의 새 작업으로 뜸(안 그러면 위젯 누를 때 앱까지 같이 열림).
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        // 라벨("할 일 추가") 글씨가 생기면서, 아이콘 부분만이 아니라 이 위젯
        // 전체(글씨+아이콘)를 눌러도 입력창이 뜨게 함 — 예전엔 아이콘(꽉 채운
        // 버튼)만 있어서 아이콘 자체에 걸었지만, 이제는 루트에 걸어야 글씨
        // 부분을 눌러도 반응함.
        views.setOnClickPendingIntent(R.id.widget_quick_add_root, pendingIntent);

        // 위젯 2·3·4와 같은 이유로 배경도 글자색과 같은 순간에 우리가 직접
        // 판단해서 심음(라이트/다크가 어긋나 안 보이는 사고 방지) — 이 위젯도
        // 이제 글씨(라벨)가 생겨서 같은 버그 대상이 됨.
        boolean isDark = WidgetThemeHelper.isDarkMode(context);
        views.setInt(R.id.widget_quick_add_root, "setBackgroundResource",
            isDark ? R.drawable.widget_background_dark : R.drawable.widget_background_light);
        views.setTextColor(R.id.quick_add_label, WidgetThemeHelper.primaryTextColor(context));

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
