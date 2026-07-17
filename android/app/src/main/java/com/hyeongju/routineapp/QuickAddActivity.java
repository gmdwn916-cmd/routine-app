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

public class QuickAddActivity extends Activity {
    public static final String PREFS_NAME = "widget_bridge";
    public static final String KEY_PENDING_ITEMS = "pending_inbox_items";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_add);

        final EditText input = findViewById(R.id.quick_add_input);

        input.requestFocus();
        // windowSoftInputMode="stateAlwaysVisible"(매니페스트)만으로는 기종에 따라
        // 키보드가 안 뜨는 경우가 있어서, 뷰가 실제로 붙은 뒤 명시적으로 한 번 더 요청.
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

        // 쉼표(,)를 입력하면 그때까지 쓴 글자를 바로 미배치에 추가하고 입력칸은
        // 비운 채로 계속 이어서 입력할 수 있게 함(2026-07-18 추가, 웹 쪽
        // submitQuickAdd의 쉼표 처리와 같은 목적) — "빨래,청소,운동"처럼 쉼표로
        // 이어 치면 팝업을 안 닫고도 하나씩 바로 쌓임. input.setText()가 이
        // 리스너를 다시 부르는 걸 막기 위해 그 순간만 리스너를 뗐다 다시 붙임.
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String text = s.toString();
                if (text.indexOf(',') < 0) return;
                String[] parts = text.split(",", -1);
                for (int i = 0; i < parts.length - 1; i++) {
                    String part = parts[i].trim();
                    if (!part.isEmpty()) addPendingItem(part);
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
        addPendingItem(text);
        Toast.makeText(this, "미배치에 추가됐어요", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void addPendingItem(String text) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String raw = prefs.getString(KEY_PENDING_ITEMS, "[]");
        JSONArray arr;
        try {
            arr = new JSONArray(raw);
        } catch (JSONException e) {
            arr = new JSONArray();
        }
        arr.put(text);
        prefs.edit().putString(KEY_PENDING_ITEMS, arr.toString()).apply();
    }
}
