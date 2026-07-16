package com.hyeongju.routineapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

// 근무 알람 하나를 실제로 예약/취소하는 공통 로직 — 여러 개를 한 번에
// 예약하는 ShiftAlarmPlugin.scheduleAll()과, 알람 화면에서 "다시 울리기"를
// 눌렀을 때 하나만 다시 예약하는 ShiftAlarmActivity 둘 다 이걸 씀(같은
// 방식으로 PendingIntent를 만들어야 나중에 취소할 때도 같은 걸 가리킬 수
// 있음 — requestCode/action이 조금이라도 다르면 서로 다른 알람으로 취급돼
// 취소가 안 됨).
class ShiftAlarmScheduler {
    static void scheduleOne(Context context, int alarmId, long epochMillis, String shiftName) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent operationIntent = new Intent(context, ShiftAlarmReceiver.class);
        operationIntent.putExtra(ShiftAlarmReceiver.EXTRA_ALARM_ID, alarmId);
        operationIntent.putExtra(ShiftAlarmReceiver.EXTRA_SHIFT_NAME, shiftName);
        PendingIntent operation = PendingIntent.getBroadcast(
            context, alarmId, operationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 상태 표시줄의 알람 아이콘을 누르면 앱이 열리게 함(진짜 알람시계
        // 앱들과 같은 동작) — 안드로이드가 요구하는 값이라 꼭 있어야 함.
        Intent showIntent = new Intent(context, MainActivity.class);
        showIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent showOperation = PendingIntent.getActivity(
            context, alarmId, showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // setAlarmClock()은 "진짜 알람시계" 용도로 만들어진 API라 — 안드로이드
        // 12+에서 정확한 시각 알람에 보통 필요한 별도 권한(SCHEDULE_EXACT_ALARM)
        // 없이도 항상 정확한 시각에 울리고, 기기가 절전(Doze) 상태여도 깨움 —
        // 이 앱이 원하는 "진짜 기상 알람" 동작과 정확히 맞아서 이걸 씀(사용자에게
        // 알림 권한 외에 추가로 받아야 하는 권한이 없다는 뜻이기도 함).
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(epochMillis, showOperation);
        am.setAlarmClock(info, operation);
    }

    static void cancelOne(Context context, int alarmId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent operationIntent = new Intent(context, ShiftAlarmReceiver.class);
        PendingIntent operation = PendingIntent.getBroadcast(
            context, alarmId, operationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        am.cancel(operation);
        operation.cancel();
    }
}
