package com.hyeongju.routineapp;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
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
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID);
        return new TodayRemoteViewsFactory(getApplicationContext(), appWidgetId);
    }

    private static class TodayRemoteViewsFactory implements RemoteViewsFactory {
        // 할 일이 몇 개 안 돼서 목록이 위젯 칸을 다 못 채우면, 그 아래 남는
        // 빈자리는 어떤 목록 줄에도 안 속해서 눌러도 반응이 없었음(2026-07-16
        // 사용자 신고) — 실제 항목 뒤에 "빈 줄"을 만들어서 그 자리도 항상 눌리는
        // 진짜 목록 줄로 채움(안 그러면 그 빈자리는 그냥 ListView의 아무것도
        // 없는 공간이라 아무 반응도 못 만듦).
        // **몇 개를 붙일지(2026-07-18, 네 번째 조정 — 위젯 크기에 맞춰 동적으로)**:
        // ① 처음엔 고정 12개 → 항목이 적을 때 위젯보다 빈 줄이 훨씬 많아져서
        // "당겨서 스크롤"되는 상태가 됨. ② 그다음 위젯 크기(dp)를 읽어서 목록
        // 자체의 높이(setViewLayoutHeight)를 항목 수에 맞춰 강제로 계산했는데,
        // 항목이 위젯을 넘칠 만큼 많을 때 목록이 실제 위젯 크기보다 커지도록
        // 요청해버려서 스크롤이 깨지고 "+" 버튼까지 화면 밖으로 밀려나는 심각한
        // 회귀가 있어서 당일 되돌림(TodayWidgetProvider.updateOne()의 2026-07-18
        // 기록 참고). ③ 결국 고정값 1로 되돌렸는데, 위젯이 넉넉히 큰데 항목이
        // 1~2개뿐이면 "목록 바로 밑 한 줄 밑"은 눌러도 반응이 없다는 재신고가
        // 있었음(예: 4줄 보이는 위젯에 1개만 있으면 2번째 줄까지만 눌리고
        // 3·4번째 줄은 무반응). **②와 다른 점**: ②는 "목록 컨테이너 자체의
        // 높이"를 강제로 계산해서 위젯 크기보다 커지면 그대로 넘쳐버리는 게
        // 문제였는데, 이번엔 목록(today_list)은 여전히 weight=1로 그대로 두고
        // (위젯 크기에 맞춰 항상 정확히 채워지고 스크롤도 항상 보장됨) 그 안에
        // "빈 줄이 몇 개 들어갈지"만 위젯 크기로 어림잡아 정함 — 어림값이 좀
        // 틀려도(더 크게 잡히면) 그냥 스크롤할 빈 공간이 조금 더 있는 것뿐이라
        // ①의 "당겨서 스크롤" 정도의 불편함으로 그침(②처럼 버튼이 화면 밖으로
        // 밀려나거나 스크롤 자체가 깨지는 일은 구조적으로 없음 — 목록 컨테이너
        // 크기 자체를 안 건드리기 때문). 그래서 이번엔 재시도해도 안전하다고
        // 판단함. 계산: AppWidgetManager.getAppWidgetOptions()의
        // OPTION_APPWIDGET_MIN_HEIGHT(세로로 놓았을 때 위젯 높이, dp)에서
        // 머리글·"+" 버튼·여백 몫(대략 100dp)을 뺀 나머지를 ROW_HEIGHT_DP로
        // 나눠 "이 위젯에 보일 수 있는 대략적인 줄 수"를 구하고, 실제 항목
        // 수를 뺀 만큼을 빈 줄로 채움(최소 1줄은 항상 보장, 혹시 모를 극단값
        // 방지로 최대 12줄까지만 허용).
        private static final int ROW_HEIGHT_DP = 30;
        private static final int HEADER_AND_BUTTON_OVERHEAD_DP = 100;
        private static final int MAX_FILLER_COUNT = 12;

        private final Context context;
        private final int appWidgetId;
        private List<JSONObject> items = new ArrayList<>();

        TodayRemoteViewsFactory(Context context, int appWidgetId) {
            this.context = context;
            this.appWidgetId = appWidgetId;
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
                            // 완료된 항목을 목록에 넣을지는 이제 JS
                            // (buildTodayWidgetPayload, 설정 탭 "완료된 항목" 토글)가
                            // 미리 결정해서 넘김(2026-07-20) — 여기서 done 여부로
                            // 다시 걸러내지 않고 받은 그대로 씀. getViewAt()이 done이면
                            // 회색+취소선으로 그려줌.
                            if (it != null) next.add(it);
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

        // 이 위젯 인스턴스가 보여줄 수 있는 대략적인 줄 수를 어림잡음 — 위 긴
        // 주석의 계산 방식. appWidgetId가 없거나(드묾) 값을 못 읽으면 안전하게
        // 최소값(1)만 반환.
        private int estimateVisibleRows() {
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return 1;
            try {
                Bundle opts = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId);
                int heightDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
                if (heightDp <= 0) return 1;
                int rows = (heightDp - HEADER_AND_BUTTON_OVERHEAD_DP) / ROW_HEIGHT_DP;
                return Math.max(1, rows);
            } catch (Exception e) {
                return 1;
            }
        }

        @Override
        public int getCount() {
            // 할 일이 아예 0개면 개수도 0 그대로 둬야 함 — TodayWidgetProvider가
            // setEmptyView로 걸어둔 "오늘 할 일이 없어요" 문구(today_empty)는
            // 개수가 정확히 0일 때만 뜨는 방식이라, 여기서 빈 줄까지 더해버리면
            // 그 문구가 영영 안 뜨게 됨. 할 일이 1개 이상 있을 때만 빈 줄을 덧붙임.
            if (items.isEmpty()) return 0;
            int fillerCount = Math.max(1, Math.min(MAX_FILLER_COUNT, estimateVisibleRows() - items.size()));
            return items.size() + fillerCount;
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
            row.setTextColor(textId, done
                ? ContextCompat.getColor(context, R.color.widget_text_secondary)
                : WidgetThemeHelper.primaryTextColor(context));
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
