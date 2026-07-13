package com.hyeongju.routineapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;
import org.json.JSONObject;

// 위젯(QuickAddActivity)이 SharedPreferences에 쌓아둔 "임시 우편함"을 웹(index.html)
// 쪽 JS가 읽어서 state.inbox로 옮길 수 있게 해주는 다리 역할만 함 — 저장 형식이나
// 미배치 데이터 구조는 전혀 모름(그냥 글자 목록을 그대로 넘겨줄 뿐), 실제 inbox
// 항목으로 바꾸는 건 JS쪽(syncWidgetInboxItems)이 담당.
@CapacitorPlugin(name = "WidgetBridge")
public class WidgetBridgePlugin extends Plugin {

    @PluginMethod
    public void getPendingItems(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(
            QuickAddActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(QuickAddActivity.KEY_PENDING_ITEMS, "[]");
        JSObject ret = new JSObject();
        JSArray items = new JSArray();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                items.put(arr.getString(i));
            }
        } catch (Exception e) {
            // 파싱 실패 시 빈 목록으로 — 다음에 위젯에서 새로 쌓으면 됨
        }
        ret.put("items", items);
        call.resolve(ret);
    }

    @PluginMethod
    public void clearPendingItems(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(
            QuickAddActivity.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(QuickAddActivity.KEY_PENDING_ITEMS, "[]").apply();
        call.resolve();
    }

    // 이번 달 달력 위젯용 — JS가 근무 계산을 전부 마친 결과(월 라벨, 요일 헤더,
    // 날짜별 근무색)를 그대로 저장해두고 위젯을 즉시 다시 그리게 함. 이 플러그인은
    // 그 데이터가 무슨 뜻인지 전혀 모름(그냥 JSON 그대로 저장) — 근무 계산 로직은
    // 절대 여기(네이티브)에 두지 않음(index.html과 두 곳에 있으면 나중에 어긋날 위험).
    // JS가 중첩 배열을 직접 넘기지 않고 JSON 문자열 하나로 뭉쳐서 보냄(call.getData()로
    // 깊은 구조를 그대로 넘기면 브리지에서 조용히 깨지는 문제가 있었음) — 여기서
    // 문자열 그대로 저장하고, 개수만 확인해서 토스트로 보여줌(원격으로 로그를 볼 수
    // 없어서 눈으로 확인하는 용도).
    @PluginMethod
    public void setMonthCalendarData(PluginCall call) {
        String json = call.getString("json");
        if (json == null) {
            Toast.makeText(getContext(), "위젯 갱신 실패: 데이터 없음", Toast.LENGTH_SHORT).show();
            call.reject("json missing");
            return;
        }

        int monthCount = -1;
        try {
            monthCount = new JSONObject(json).optJSONArray("months").length();
        } catch (Exception e) {
            Toast.makeText(getContext(), "위젯 갱신 실패: JSON 파싱 오류 - " + e.getMessage(), Toast.LENGTH_LONG).show();
            call.reject("bad json", e);
            return;
        }

        SharedPreferences prefs = getContext().getSharedPreferences(
            MonthCalendarWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
        // 앱이 새 자료를 보낼 때마다 위젯이 보던 달을 "이번달"(배열 인덱스 1)로
        // 되돌림 — 위젯에서 이전/다음 달로 넘겨봤어도, 앱을 열면 항상 오늘 기준으로
        // 다시 시작하는 게 자연스러움.
        prefs.edit()
            .putString(MonthCalendarWidgetProvider.KEY_MONTH_DATA, json)
            .putInt(MonthCalendarWidgetProvider.KEY_DISPLAY_INDEX, 1)
            .apply();
        MonthCalendarWidgetProvider.refreshAll(getContext());
        Toast.makeText(getContext(), "위젯 갱신됨 (" + monthCount + "개월치)", Toast.LENGTH_SHORT).show();
        call.resolve();
    }
}
