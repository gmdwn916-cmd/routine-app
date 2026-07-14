package com.hyeongju.routineapp;

import android.content.Context;
import android.content.SharedPreferences;

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
    // 깊은 구조를 그대로 넘기면 브리지에서 조용히 깨지는 문제가 있었음).
    @PluginMethod
    public void setMonthCalendarData(PluginCall call) {
        String json = call.getString("json");
        if (json == null) {
            call.reject("json missing");
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
        call.resolve();
    }

    // N주 스케줄 위젯용 — 위젯 2와 같은 원칙(JS가 근무·할일 계산을 전부 마친
    // 결과만 저장하고 그리기만 함, 네이티브에 로직 없음).
    @PluginMethod
    public void setScheduleData(PluginCall call) {
        String json = call.getString("json");
        if (json == null) {
            call.reject("json missing");
            return;
        }
        SharedPreferences prefs = getContext().getSharedPreferences(
            ScheduleWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
        // 앱이 새 자료를 보낼 때마다 페이지 위치를 1(=배열의 "이번 2주")로
        // 되돌림 — 위젯에서 이전/다음 페이지로 넘겨봤어도 앱을 열면 오늘
        // 기준으로 리셋됨(달력 위젯이 매번 인덱스 1로 되돌리는 것과 동일).
        prefs.edit()
            .putString(ScheduleWidgetProvider.KEY_SCHEDULE_DATA, json)
            .putInt(ScheduleWidgetProvider.KEY_PAGE_INDEX, 1)
            .apply();
        ScheduleWidgetProvider.refreshAll(getContext());
        call.resolve();
    }

    // 오늘 전체 관리 위젯(위젯 4)용 — 오늘의 근무·반복 할일·한 번짜리 할 일을
    // JS가 전부 계산해서 순서까지 정해 넘겨준 결과를 저장하고 다시 그리게 함.
    // 이 위젯은 목록(ListView) 기반이라 텍스트 몇 개를 세팅하는 다른 위젯과
    // 달리 TodayWidgetProvider.refreshAll()이 목록 갱신(notifyAppWidgetViewDataChanged)
    // 까지 같이 처리한다.
    @PluginMethod
    public void setTodayWidgetData(PluginCall call) {
        String json = call.getString("json");
        if (json == null) {
            call.reject("json missing");
            return;
        }
        SharedPreferences prefs = getContext().getSharedPreferences(
            TodayWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(TodayWidgetProvider.KEY_TODAY_DATA, json).apply();
        TodayWidgetProvider.refreshAll(getContext());
        call.resolve();
    }

    // 위젯 4에서 체크를 누르면 그 자리에서 바로 화면은 바뀌지만(낙관적 갱신),
    // 진짜 데이터(done_log/ev.done)에 반영하는 건 여기 쌓인 "이 항목을 이
    // 상태로 만들어라"(토글이 아니라 최종 상태) 목록을 JS가 앱을 열 때 읽어가서
    // 처리한다 — 위젯 1의 "임시 우편함"과 같은 다리 역할.
    @PluginMethod
    public void getPendingTodayToggles(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(
            TodayWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(TodayWidgetProvider.KEY_PENDING_TOGGLES, "[]");
        JSObject ret = new JSObject();
        JSArray toggles = new JSArray();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject t = arr.getJSONObject(i);
                JSObject jt = new JSObject();
                jt.put("id", t.optString("id", ""));
                jt.put("date", t.optString("date", ""));
                jt.put("type", t.optString("type", ""));
                jt.put("done", t.optBoolean("done", false));
                toggles.put(jt);
            }
        } catch (Exception e) {
            // 파싱 실패 시 빈 목록으로 — 다음에 위젯에서 새로 쌓으면 됨
        }
        ret.put("toggles", toggles);
        call.resolve(ret);
    }

    @PluginMethod
    public void clearPendingTodayToggles(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(
            TodayWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(TodayWidgetProvider.KEY_PENDING_TOGGLES, "[]").apply();
        call.resolve();
    }

    // 미배치 위젯(위젯 5)용 — 근무/날짜 계산이 아예 없는 가장 단순한 위젯이라
    // JS가 넘겨준 목록(count, items)을 그대로 저장하고 다시 그리게만 함.
    // 읽기 전용이라 임시 보관함(pending toggles) 같은 것도 필요 없음.
    @PluginMethod
    public void setInboxWidgetData(PluginCall call) {
        String json = call.getString("json");
        if (json == null) {
            call.reject("json missing");
            return;
        }
        SharedPreferences prefs = getContext().getSharedPreferences(
            InboxWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(InboxWidgetProvider.KEY_INBOX_DATA, json).apply();
        InboxWidgetProvider.refreshAll(getContext());
        call.resolve();
    }

    // 데이터 내보내기(백업)용 — 네이티브 앱 안의 웹뷰는 일반 브라우저와 달리
    // "파일 다운로드"를 자체적으로 처리하는 기능이 없어서, blob 링크를 눌러도
    // 아무 일도 안 일어남. 그래서 네이티브에서는 이 경로 대신 안드로이드가
    // 다운로드 폴더에 직접 파일을 써주는 표준 방식(MediaStore)을 씀. 웹(브라우저)
    // 쪽은 그대로 기존 blob 다운로드 방식을 계속 씀 — 거긴 문제없이 잘 됨.
    @PluginMethod
    public void saveBackupFile(PluginCall call) {
        String filename = call.getString("filename");
        String content = call.getString("content");
        if (filename == null || content == null) {
            call.reject("filename/content missing");
            return;
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, filename);
                values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/json");
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 1);
                android.content.ContentResolver resolver = getContext().getContentResolver();
                android.net.Uri uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    call.reject("insert failed");
                    return;
                }
                java.io.OutputStream os = resolver.openOutputStream(uri);
                try {
                    os.write(content.getBytes("UTF-8"));
                } finally {
                    os.close();
                }
                values.clear();
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
            } else {
                java.io.File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                java.io.File file = new java.io.File(downloadsDir, filename);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                try {
                    fos.write(content.getBytes("UTF-8"));
                } finally {
                    fos.close();
                }
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("save failed: " + e.getMessage(), e);
        }
    }
}
