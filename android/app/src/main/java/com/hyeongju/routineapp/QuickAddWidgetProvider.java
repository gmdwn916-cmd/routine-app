package com.hyeongju.routineapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

public class QuickAddWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
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
            boolean isDark = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            views.setInt(R.id.widget_quick_add_root, "setBackgroundResource",
                isDark ? R.drawable.widget_background_dark : R.drawable.widget_background_light);
            views.setTextColor(R.id.quick_add_label, ContextCompat.getColor(context, R.color.widget_text_primary));

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
