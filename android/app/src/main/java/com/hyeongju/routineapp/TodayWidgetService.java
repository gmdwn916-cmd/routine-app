package com.hyeongju.routineapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// 오늘 위젯(위젯 4)의 스크롤 목록을 실제로 채우는 부분. 안드로이드 위젯 안에
// 스크롤되는 목록을 넣으려면 이 방식(RemoteViewsService + RemoteViewsFactory)이
// 필요함 — 위젯 2·3처럼 고정된 칸 몇 개를 미리 그려두는 방식으로는 "항목 개수가
// 매번 달라지고 스크롤도 되는 목록"을 만들 수 없어서 이 위젯에서 처음 씀.
public class TodayWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TodayRemoteViewsFactory(getApplicationContext());
    }

    private static class TodayRemoteViewsFactory implements RemoteViewsFactory {
        // 할 일이 몇 개 안 돼서 목록이 위젯 칸을 다 못 채우면, 그 아래 남는
        // 빈자리는 어떤 목록 줄에도 안 속해서 눌러도 반응이 없었음(2026-07-16
        // 사용자 신고) — 실제 항목 뒤에 "빈 줄"을 이만큼 더 만들어서 그 자리도
        // 항상 눌리는 진짜 목록 줄로 채움(안 그러면 그 빈자리는 그냥 ListView의
        // 아무것도 없는 공간이라 아무 반응도 못 만듦). 웬만큼 큰 위젯 크기까지
        // 커버하도록 넉넉히 잡음 — 화면에 안 보이는 만큼은 그려지지 않아 비용도
        // 거의 없음.
        private static final int FILLER_COUNT = 12;

        private final Context context;
        private List<JSONObject> items = new ArrayList<>();

        TodayRemoteViewsFactory(Context context) {
            this.context = context;
        }

        @Override
        public void onCreate() {
        }

        // 목록 줄 하나하나를 만들기 전에 딱 한 번 불림 — 무거운 일(데이터 읽기)은
        // 여기서 미리 끝내두고, getViewAt()은 이미 읽어둔 목록에서 꺼내 쓰기만 함.
        @Override
        public void onDataSetChanged() {
            List<JSONObject> next = new ArrayList<>();
            SharedPreferences prefs = context.getSharedPreferences(
                TodayWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
            String raw = prefs.getString(TodayWidgetProvider.KEY_TODAY_DATA, null);
            if (raw != null) {
                try {
                    JSONArray arr = new JSONObject(raw).optJSONArray("items");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject it = arr.optJSONObject(i);
                            // 완료된 항목은 목록에서 아예 안 보이게 함 — payload는
                            // 원래 미완료 항목만 담고 있지만, 혹시 남아있는 옛 데이터를
                            // 대비한 안전장치로 한 번 더 걸러줌.
                            if (it != null && !it.optBoolean("done", false)) next.add(it);
                        }
                    }
                } catch (Exception e) {
                    // 데이터가 깨져 있으면 빈 목록으로
                }
            }
            items = next;
        }

        @Override
        public void onDestroy() {
            items.clear();
        }

        @Override
        public int getCount() {
            // 할 일이 아예 0개면 개수도 0 그대로 둬야 함 — TodayWidgetProvider가
            // setEmptyView로 걸어둔 "오늘 할 일이 없어요" 문구(today_empty)는
            // 개수가 정확히 0일 때만 뜨는 방식이라, 여기서 빈 줄까지 더해버리면
            // (0+12) 그 문구가 영영 안 뜨게 됨. 할 일이 1개 이상 있을 때만 빈
            // 줄을 덧붙임.
            return items.isEmpty() ? 0 : items.size() + FILLER_COUNT;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_today_item);

            // 실제 항목 뒤에 붙는 빈 줄 — 체크칸은 숨기고(자리만 차지, 안 보임)
            // 글자도 비운 채로, 줄 전체를 누르면 오늘 탭이 열리게만 함(토글 대상
            // 항목 자체가 없으니 체크 반응은 없음).
            if (position >= items.size()) {
                row.setTextViewText(idFor("item_text"), "");
                row.setViewVisibility(idFor("item_check"), View.INVISIBLE);
                Intent fillerOpenIntent = new Intent();
                fillerOpenIntent.putExtra(TodayWidgetProvider.EXTRA_OPEN_APP, true);
                // row_root뿐 아니라 item_text에도 똑같이 걸어야 함 — 위
                // "체크칸을 제외한 줄의 나머지 부분" 주석에서 확인했듯 자식은
                // 부모의 fillInIntent를 물려받지 않음.
                row.setOnClickFillInIntent(idFor("row_root"), fillerOpenIntent);
                row.setOnClickFillInIntent(idFor("item_text"), fillerOpenIntent);
                return row;
            }

            JSONObject it = items.get(position);
            String id = it.optString("id", "");
            String text = it.optString("text", "");
            String icon = it.optString("icon", "");
            String type = it.optString("type", "once");
            boolean done = it.optBoolean("done", false);

            int textId = idFor("item_text");
            row.setTextViewText(textId, (icon.isEmpty() ? "" : icon + " ") + text);
            row.setInt(textId, "setPaintFlags",
                done ? (Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG) : Paint.ANTI_ALIAS_FLAG);
            row.setTextColor(textId, ContextCompat.getColor(context,
                done ? R.color.widget_text_secondary : R.color.widget_text_primary));
            row.setImageViewResource(idFor("item_check"),
                done ? R.drawable.widget_check_on : R.drawable.widget_check_off);
            // 빈 줄(위 position >= items.size() 분기)이 체크칸을 INVISIBLE로
            // 숨기는데, 안드로이드가 같은 레이아웃을 쓰는 줄끼리 뷰를 재활용할 때
            // 그 상태가 그대로 남아 실제 항목 줄인데도 체크칸이 안 보이는 문제가
            // 있었음(다크 모드에서 자주 눈에 띔 — 2026-07-16 발견·수정) — 실제
            // 항목은 항상 명시적으로 VISIBLE로 되돌림.
            row.setViewVisibility(idFor("item_check"), View.VISIBLE);

            // 체크칸(item_check)이 탭됐을 때 어떤 항목의 무엇을 바꿔야 하는지 알려주는
            // 꼬리표. "done을 뒤집어라"가 아니라 "눌렀을 때 이 상태(!done)가 될
            // 것이다"를 미리 담아 보냄 — TodayWidgetProvider.handleToggle()이 이
            // 값을 그대로 최종 상태로 저장하므로 중복 처리돼도 결과가 달라지지
            // 않음. 예전엔 줄 전체(row_root)가 눌려도 체크됐는데, 텍스트를 눌렀을
            // 때도 체크가 토글돼서 불편하다는 요청으로 체크칸(item_check)에만
            // 반응하도록 좁힘 — 텍스트(item_text)는 이제 눌러도 아무 반응 없음.
            Intent fillInIntent = new Intent();
            fillInIntent.putExtra(TodayWidgetProvider.EXTRA_ITEM_ID, id);
            fillInIntent.putExtra(TodayWidgetProvider.EXTRA_ITEM_TYPE, type);
            fillInIntent.putExtra(TodayWidgetProvider.EXTRA_NEW_DONE, !done);
            row.setOnClickFillInIntent(idFor("item_check"), fillInIntent);

            // 체크칸을 제외한 줄의 나머지 부분(row_root의 여백 + item_text, 글자
            // 없는 빈 공백 포함)을 누르면 앱을 오늘 탭으로 염(2026-07-16, 그리고
            // 같은 날 "빈 공백 부분은 안 눌린다"는 재확인으로 item_text에도 같은
            // fillInIntent를 직접 달아줌 — 컬렉션 위젯은 자식이 부모의
            // fillInIntent를 자동으로 물려받지 않고, 각자 자기 몫의 fillInIntent가
            // 있어야만 반응하는 것으로 확인됨. row_root만 걸어두면 item_check가
            // 차지한 칸을 뺀 "여백(패딩)" 부분만 반응하고, 그 안의 item_text(글자
            // 있는 곳과 글자 없는 빈 공백 전부 포함하는 넓은 칸)는 자기 몫이 없어서
            // 안 눌렸음.
            Intent openFillInIntent = new Intent();
            openFillInIntent.putExtra(TodayWidgetProvider.EXTRA_OPEN_APP, true);
            row.setOnClickFillInIntent(idFor("row_root"), openFillInIntent);
            row.setOnClickFillInIntent(idFor("item_text"), openFillInIntent);

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
