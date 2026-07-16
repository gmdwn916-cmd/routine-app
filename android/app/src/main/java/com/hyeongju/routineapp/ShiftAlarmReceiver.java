package com.hyeongju.routineapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

// 근무유형별 알람이 실제로 울릴 시각에 안드로이드(AlarmManager)가 이걸 깨움
// (예약은 ShiftAlarmScheduler 참고). 여기서 화면을 직접 띄우지 않고(백그라운드
// 상태에서 액티비티를 바로 띄우는 건 안드로이드 10+부터 막혀 있음), "전체
// 화면 인텐트"가 달린 알림을 하나 올려서 안드로이드가 알아서 화면을 띄우게
// 함 — 전화 수신 화면과 같은 원리. 화면이 꺼져있거나 잠겨있으면 안드로이드가
// 그 알림의 fullScreenIntent(ShiftAlarmActivity)를 자동으로 전체 화면으로
// 띄워주고, 화면이 켜져 있고 잠금이 풀려있으면 대신 위에서 살짝 뜨는 알림으로
// 보여주고 눌러야 열림 — 둘 다 안드로이드가 정한 동작이라 앱에서 더 강제할
// 수 없음.
public class ShiftAlarmReceiver extends BroadcastReceiver {
    static final String EXTRA_ALARM_ID = "alarm_id";
    static final String EXTRA_SHIFT_NAME = "shift_name";
    private static final String CHANNEL_ID = "shift-alarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra(EXTRA_ALARM_ID, 0);
        String shiftName = intent.getStringExtra(EXTRA_SHIFT_NAME);
        if (shiftName == null) shiftName = "";

        Intent fullScreenIntent = new Intent(context, ShiftAlarmActivity.class);
        fullScreenIntent.putExtra(EXTRA_ALARM_ID, alarmId);
        fullScreenIntent.putExtra(EXTRA_SHIFT_NAME, shiftName);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_CLEAR_TOP
            | Intent.FLAG_ACTIVITY_NO_HISTORY);
        // 알람 id마다 다른 requestCode를 줘서 서로 다른 알람의 인텐트가 안
        // 겹치게 함(이 프로젝트에서 위젯 인텐트를 만들 때 항상 지키는 습관과
        // 같은 이유 — requestCode/action이 같으면 나중 것이 앞의 것을 덮어씀).
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
            context, alarmId, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_add)
            .setContentTitle("근무 알람")
            .setContentText(shiftName.isEmpty() ? "근무일이에요" : (shiftName + " 근무일이에요"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent);

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        try {
            nm.notify(alarmId, builder.build());
        } catch (SecurityException e) {
            // 알림 권한이 아직 없으면 조용히 무시 — 권한을 받으면 다음 예약부터 정상 동작
        }
    }
}
