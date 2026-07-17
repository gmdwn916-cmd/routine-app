package com.hyeongju.routineapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// 오늘 할일 위젯의 + 버튼에서 뜨는 입력창(2026-07-16) — 화면 모양·동작은
// QuickAddActivity(할일추가 위젯)와 완전히 동일(입력칸 하나 + 엔터로 저장,
// 다른 정보 표시 없음. 처음엔 DayQuickViewActivity를 재사용해서 날짜·근무·
// 할일 목록까지 같이 보여줬는데, 사용자가 "다른거 보여주지 말고 할일추가
// 위젯처럼만 띄워줘"로 요청해서 이 화면으로 바꿈) — 다만 저장 위치만 다름:
// 미배치가 아니라 그 날짜(오늘)에 바로 배치된 할 일로 들어감. 저장소는
// DayQuickViewActivity와 같은 pending_dated_items를 그대로 공유해서 씀
// (새 임시 우편함을 또 만들지 않음, JS의 syncWidgetDatedItems()가 그대로 처리).
public class TodayQuickAddActivity extends Activity {
    static final String EXTRA_DATE = "date";

    private String targetDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_add);

        targetDate = getIntent().getStringExtra(EXTRA_DATE);
        if (TextUtils.isEmpty(targetDate)) {
            // 위젯이 아직 오늘 날짜를 못 넘겨준 극히 드문 경우를 대비한 안전장치 —
            // 근무 계산이 아니라 단순 "오늘이 며칠인지"라 네이티브에서 계산해도 되는
            // 예외(TodayWidgetProvider의 같은 fallback과 동일한 이유).
            targetDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(new java.util.Date());
        }

        final EditText input = findViewById(R.id.quick_add_input);

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

        // 쉼표(,)를 입력하면 그때까지 쓴 글자를 바로 오늘 할 일로 추가하고
        // 입력칸은 비운 채로 계속 이어서 입력할 수 있게 함(2026-07-18 추가,
        // QuickAddActivity와 같은 이유).
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
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

    private void save(EditText input) {
        String text = input.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            finish();
            return;
        }
        addPendingDatedItem(text, targetDate);
        Toast.makeText(this, "오늘 할 일에 추가됐어요", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void addPendingDatedItem(String text, String date) {
        SharedPreferences prefs = getSharedPreferences(
            DayQuickViewActivity.PREFS_NAME, MODE_PRIVATE);
        String raw = prefs.getString(DayQuickViewActivity.KEY_PENDING_DATED_ITEMS, "[]");
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
        prefs.edit().putString(DayQuickViewActivity.KEY_PENDING_DATED_ITEMS, arr.toString()).apply();
    }
}
