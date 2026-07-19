package com.hyeongju.routineapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// 스케줄 위젯에서 날짜 칸을 누르면 뜨는 작은 화면(2026-07-16 신규) — 그
// 날짜의 근무·할 일을 보여주기만 하고(위젯이 이미 그리고 있던 정보를 그대로
// 인텐트로 받음, 따로 다시 계산 안 함), 입력칸에 쓰고 엔터 치면 그 날짜에
// 바로 새 할 일이 추가됨. QuickAddActivity와 같은 "작은 다이얼로그처럼 뜨는
// 화면 + 임시 보관함에 쌓아뒀다가 앱이 열릴 때 진짜 데이터에 반영" 패턴을
// 그대로 씀 — 이번엔 미배치가 아니라 "이 날짜에 바로 배치"라는 점만 다름.
public class DayQuickViewActivity extends Activity {
    public static final String PREFS_NAME = "widget_bridge";
    public static final String KEY_PENDING_DATED_ITEMS = "pending_dated_items";

    static final String EXTRA_DATE = "date";
    static final String EXTRA_DATE_LABEL = "date_label";
    static final String EXTRA_SHIFT_NAME = "shift_name";
    static final String EXTRA_SHIFT_COLOR = "shift_color";
    static final String EXTRA_TODOS = "todos_json";

    private String targetDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_quick_view);

        targetDate = getIntent().getStringExtra(EXTRA_DATE);

        TextView dateLabel = findViewById(R.id.dqv_date_label);
        dateLabel.setText(getIntent().getStringExtra(EXTRA_DATE_LABEL));

        TextView shiftLabel = findViewById(R.id.dqv_shift_label);
        String shiftName = getIntent().getStringExtra(EXTRA_SHIFT_NAME);
        String shiftColor = getIntent().getStringExtra(EXTRA_SHIFT_COLOR);
        if (!TextUtils.isEmpty(shiftName)) {
            shiftLabel.setText(shiftName);
            shiftLabel.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(shiftColor)) {
                try {
                    int base = Color.parseColor(shiftColor);
                    // 위젯의 근무 배지와 같은 느낌: 배경은 옅게(약 15%), 글자는 근무색 그대로.
                    // 이건 위젯(RemoteViews)이 아니라 진짜 액티비티라 setColorFilter
                    // 우회 없이 GradientDrawable로 바로 둥근 모서리를 줄 수 있음.
                    int tintedBg = (base & 0x00FFFFFF) | 0x26000000;
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(tintedBg);
                    bg.setCornerRadius(dp(10));
                    shiftLabel.setBackground(bg);
                    shiftLabel.setTextColor(base | 0xFF000000);
                } catch (IllegalArgumentException e) {
                    // 색상 파싱 실패 시 배경 없이 글자만 표시
                }
            }
        }

        TextView todosView = findViewById(R.id.dqv_todos);
        String todosJson = getIntent().getStringExtra(EXTRA_TODOS);
        StringBuilder sb = new StringBuilder();
        try {
            if (todosJson != null) {
                JSONArray arr = new JSONArray(todosJson);
                for (int i = 0; i < arr.length(); i++) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(arr.optString(i, ""));
                }
            }
        } catch (JSONException e) {
            // 무시 — 빈 목록으로 표시됨
        }
        if (sb.length() == 0) {
            todosView.setText("이 날 할 일이 없어요");
            todosView.setTextColor(ContextCompat.getColor(this, R.color.widget_text_secondary));
        } else {
            todosView.setText(sb.toString());
        }

        // 카드(입력칸 제외) 아무 곳이나 누르면 앱을 그 날짜 상세 화면으로
        // 열어줌(2026-07-19 추가, 사용자 요청) — dqv_input은 자기 터치를
        // 직접 소비(포커스+커서 배치)하므로 이 리스너가 안 불리고, 나머지
        // 영역(날짜·근무·할 일 목록, 여백)만 여기로 떨어짐(안드로이드 표준
        // 터치 처리 — 스케줄 위젯의 날짜 칸/부모 클릭 분리와 같은 원리).
        if (!TextUtils.isEmpty(targetDate)) {
            View root = findViewById(R.id.dqv_root);
            root.setOnClickListener(v -> openAppToDay());
        }

        final EditText input = findViewById(R.id.dqv_input);
        input.requestFocus();
        input.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });

        input.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnterDown = event != null
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_DONE || isEnterDown) {
                save(input);
                return true;
            }
            return false;
        });

        // 쉼표(,)를 입력하면 그때까지 쓴 글자를 바로 이 날짜에 추가하고 입력칸은
        // 비운 채로 계속 이어서 입력할 수 있게 함(2026-07-18 추가,
        // QuickAddActivity와 같은 이유).
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(targetDate)) return;
                String text = s.toString();
                if (text.indexOf(',') < 0) return;
                String[] parts = text.split(",", -1);
                for (int i = 0; i < parts.length - 1; i++) {
                    String part = parts[i].trim();
                    if (!part.isEmpty()) addPendingDatedItem(part, targetDate);
                }
                input.removeTextChangedListener(this);
                input.setText(parts[parts.length - 1]);
                input.setSelection(input.getText().length());
                input.addTextChangedListener(this);
            }
        });
    }

    // 이 화면(taskAffinity="")과 MainActivity는 서로 다른 작업(task)이라
    // FLAG_ACTIVITY_NEW_TASK가 필요함(위젯 1·오늘 위젯의 "+" 버튼과 같은
    // 이유). MainActivity가 target="day"를 받으면(JS의 syncWidgetNavTarget)
    // 달력 탭에서 그 날짜가 있는 달로 이동한 뒤 날짜 상세 화면을 바로 엶.
    private void openAppToDay() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_WIDGET_NAV, "day");
        intent.putExtra(MainActivity.EXTRA_WIDGET_NAV_DATE, targetDate);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void save(EditText input) {
        String text = input.getText().toString().trim();
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(targetDate)) {
            finish();
            return;
        }
        addPendingDatedItem(text, targetDate);
        Toast.makeText(this, "추가했어요", Toast.LENGTH_SHORT).show();
        finish();
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void addPendingDatedItem(String text, String date) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String raw = prefs.getString(KEY_PENDING_DATED_ITEMS, "[]");
        JSONArray arr;
        try {
            arr = new JSONArray(raw);
        } catch (JSONException e) {
            arr = new JSONArray();
        }
        try {
            JSONObject item = new JSONObject();
            item.put("text", text);
            item.put("date", date);
            arr.put(item);
        } catch (JSONException e) {
            // 무시 — 이번 항목은 못 넘어갈 수 있지만 다음 시도는 정상 동작
        }
        prefs.edit().putString(KEY_PENDING_DATED_ITEMS, arr.toString()).apply();
    }
}
