package com.hyeongju.routineapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// 미배치 위젯(위젯 5)의 스크롤 목록을 실제로 채우는 부분 — 위젯 4
// (TodayWidgetService)와 같은 방식(RemoteViewsService + RemoteViewsFactory),
// 다만 체크 처리가 없어서 훨씬 단순함(텍스트만 표시, 줄마다 빈 fillInIntent만
// 붙여서 탭하면 InboxWidgetProvider가 만든 템플릿(앱 열기)이 그대로 실행됨).
public class InboxWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new InboxRemoteViewsFactory(getApplicationContext());
    }

    private static class InboxRemoteViewsFactory implements RemoteViewsFactory {
        private final Context context;
        private List<String> texts = new ArrayList<>();

        InboxRemoteViewsFactory(Context context) {
            this.context = context;
        }

        @Override
        public void onCreate() {
        }

        @Override
        public void onDataSetChanged() {
            List<String> next = new ArrayList<>();
            SharedPreferences prefs = context.getSharedPreferences(
                InboxWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
            String raw = prefs.getString(InboxWidgetProvider.KEY_INBOX_DATA, null);
            if (raw != null) {
                try {
                    JSONArray arr = new JSONObject(raw).optJSONArray("items");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject it = arr.optJSONObject(i);
                            if (it != null) next.add(it.optString("text", ""));
                        }
                    }
                } catch (Exception e) {
                    // 데이터가 깨져 있으면 빈 목록으로
                }
            }
            texts = next;
        }

        @Override
        public void onDestroy() {
            texts.clear();
        }

        // 위젯 4(오늘)의 FILLER_COUNT와 같은 이유·같은 방식(2026-07-18 추가) —
        // 항목이 몇 개 안 될 때 실제 항목 바로 아래 빈 줄 하나를 더 채워서
        // 그 자리도 눌리게 함. **이 방식은 목록(inbox_list)의 weight/높이를
        // 전혀 안 건드리고 어댑터 안에 "항목 하나 더"를 얹는 것뿐이라, 항목이
        // 많아서 스크롤이 필요한 경우에도 이 빈 줄은 그냥 맨 마지막에 자연스럽게
        // 스크롤되는 한 줄일 뿐 — 오늘 위젯이 2026-07-16부터 문제없이 써온
        // 검증된 방식**(이 위젯도 처음부터 이렇게 했어야 했는데 빠져 있었음).
        // **`RemoteViews.setViewLayoutHeight`로 목록 자체 높이를 항목 수에
        // 맞춰 강제로 계산하는 방식은 절대 다시 시도하지 말 것** — 항목이
        // 많을 때 위젯 실제 크기보다 목록이 커져서 스크롤이 깨지고 그 아래
        // "+" 버튼까지 화면 밖으로 밀려나는 심각한 회귀가 있었음(InboxWidgetProvider.
        // updateOne()의 2026-07-18 기록 참고, 그 방식은 도입 당일 되돌림).
        private static final int FILLER_COUNT = 1;

        @Override
        public int getCount() {
            // 위젯 4와 같은 이유로, 항목이 정말 0개일 때는 그대로 0을 반환해야
            // inbox_empty("미배치 항목이 없어요") 문구가 뜸 — 항목이 1개
            // 이상일 때만 빈 줄을 덧붙임.
            return texts.isEmpty() ? 0 : texts.size() + FILLER_COUNT;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_inbox_item);

            boolean isFiller = position >= texts.size();
            // 실제 항목 뒤에 붙는 빈 줄 — 글자·점(•)을 비우고, 나머지(클릭 처리
            // 등)는 실제 항목과 완전히 동일(이 위젯은 모든 줄이 항상 "앱 열기"
            // 하나뿐이라 실제 항목이든 빈 줄이든 fillInIntent가 똑같음).
            String text = isFiller ? "" : texts.get(position);
            row.setTextViewText(idFor("inbox_item_text"), text);
            // 오늘 위젯의 item_check와 같은 이유(2026-07-16 발견된 버그와 같은
            // 함정) — 같은 레이아웃을 재활용하는 컬렉션 위젯이라, 빈 줄이 점을
            // INVISIBLE로 숨긴 뒤 그 뷰가 실제 항목 줄로 재활용될 때 명시적으로
            // VISIBLE로 되돌리지 않으면 점이 계속 안 보이는 상태로 남을 수 있음
            // — 실제 항목 쪽에서 항상 VISIBLE로 되돌림.
            row.setViewVisibility(idFor("inbox_item_bullet"), isFiller ? View.INVISIBLE : View.VISIBLE);
            // 이 줄의 XML 기본 textColor(@color/widget_text_primary)는 실제 기기
            // 시스템 다크모드만 보고 자동으로 해석되므로, 앱 안 테마 설정을
            // 따르게 하려면 여기서 직접 색을 심어야 함(2026-07-17 추가 — 다른
            // 위젯들은 이미 다 이렇게 하고 있었는데 이 줄만 빠져 있었음).
            row.setTextColor(idFor("inbox_item_text"), WidgetThemeHelper.primaryTextColor(context));
            // 모든 줄이 항상 같은 동작(앱 열기)이라 특별한 값을 안 실은 빈
            // fillInIntent만 붙임 — 그래도 이게 있어야 탭에 반응함(RemoteViews
            // 컬렉션 위젯의 제약).
            row.setOnClickFillInIntent(idFor("inbox_item_row"), new Intent());
            return row;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        private int idFor(String name) {
            return context.getResources().getIdentifier(name, "id", context.getPackageName());
        }
    }
}
