# 교대근무 루틴 앱 (CLAUDE.md)

## 앱의 목적 (정체성)
교대근무자가 스케줄을 편하게 짜고, 잘 지킬 수 있게 하는 스케줄러.
루틴화는 목적이 아니라 '잘 지키기'를 위한 도구다.
- 이 앱은 '앞으로'만 본다: 오늘과 다음만 다룬다.
- 회고·성찰·통계 대시보드는 만들지 않는다(그건 노트 앱의 몫).
  성취율 숫자 정도의 '슬쩍 보는 신호'까지만 허용.

## 앱의 경계 기준 (기능 추가 판단)
"빠르게 확인·체크·처리 = 이 앱 / 앉아서 오래 정리·기록 = 다른 앱(옵시디언 등)"
판단법: 이 기능을 쓰려면 앱을 오래 붙잡아야 하나?
붙잡아야 하면 넣지 않는다. 슬쩍 보고 닫을 수 있으면 넣는다.

## 핵심 시스템 (확정 — 모든 기능·화면은 이 흐름을 따른다)
교대근무자는 '넣기'(할 일 떠올림)와 '놓기'(언제 할지)가 분리된다.
근무표를 봐야 놓을 수 있기 때문. 이 분리를 잇는 것이 이 앱의 심장.

1) 넣기: 어디서든 빈칸에 글 + 반복 O/X 버튼만으로 즉시 추가.
   - 특정 날짜 화면에서 추가하면 그 날짜에 바로 배치(넣기=놓기 동시).
   - 날짜를 모르면 '미배치(인박스)'로 들어간다.
2) 미배치(인박스): 화면 어디서든 항상 떠 있는 버튼(미처리 개수 배지).
   - 누르면 미배치 리스트 → 항목을 골라 배치.
   - 항목 상태는 딱 둘: 미배치 / 배치됨. (아래 "월 태그"는 새로운 세 번째
     상태가 아니라 미배치 상태 안에서의 부가 정보일 뿐 — 미배치 배지 개수에
     계속 포함됨. 다시 이 규칙을 어기고 진짜 세 번째 상태를 만들려 하면 안 됨.)
   - 대략 배치(월 태그): 몇 날짜에 할지는 아직 모르지만 "대충 몇 월에 하면
     되겠다"는 감만 있을 때를 위한 기능. 연간 탭에서 미배치 항목을 꾹 눌러
     드래그하면(길게 누르는 순간 미배치 창이 사라지고 뒤의 연간 화면이 드러남)
     원하는 달 박스에 놓아서 그 달로 태그할 수 있음(inbox item에
     targetMonth:"YYYY-MM" 저장). 월 박스 자체에는 아무 표시도 안 함(지저분해
     지지 않도록). 태그된 항목은 평소 미배치 목록에는 안 보이고, 그 달의
     달력(전체보기) 화면을 보면서 미배치를 열었을 때만 위쪽에 따로 나타남 —
     거기서 다시 꾹 눌러 드래그해서 원하는 날짜 칸에 놓으면 그 날짜로 확정
     배치(targetMonth 없어지고 완전한 배치됨 상태). 짧게 탭하면(드래그 아님)
     예전처럼 배치용 캘린더가 뜸 — 안 바뀜.
3) 배치: 항목당 남은 질문은 하나만(넣기의 OX가 첫 갈림길이므로).
   - 반복 X → "언제?" : 근무가 보이는 달력에서 날짜 하나 지정.
   - 반복 O → "어떤 날마다?" : 버튼 2개만 제공("기간" / "근무·요일") — 탭이
     지저분해지지 않도록 예전에 따로 있던 "요일로 고르기"는 "근무·요일" 안으로,
     "음력으로 고르기"는 "기간" 안으로 흡수함.
     a. 근무·요일로 고르기: 근무×요일 표(세로: 근무 순서대로, 가로: 월~일)를
        보여주고 칸을 누르면 바로 켜지고(색칠) 다시 누르면 꺼짐 — 그게 다임.
        "조건 추가" 같은 별도 단계나 무슨 요일마다인지 알려주는 문구 없음
        (색칠된 칸 자체가 곧 반복 조건이라 따로 설명 안 함 — 확정된 사용자
        의견). 한 근무의 요일 7칸을 전부 켜면 "그 근무 날마다"(요일 무관),
        한 요일의 근무를 전부 켜면 "그 요일마다"(근무 무관)와 동일 — 전체
        칸을 다 켜는 것만으로 예전 "요일만"/"근무만" 조건이 자연스럽게 표현됨.
     b. 기간으로 고르기: 기본은 "3개월마다" 등 년/개월/일 간격(근무 주기와
        무관, 실제 달력 날짜로 계산. 전환 시작일 = 그 할 일의 원래 날짜, 새로
        만들 때는 오늘). 누르면 숫자 세 칸 모두 0으로 시작함(하위 탭 없이,
        구석에 작은 "음력" 토글 버튼 하나만 있음 — 누르면 화면이 음력 월/일
        입력으로 바뀌고, 원래 날짜의 음력이 자동으로 채워져 있어 그대로
        확인만 눌러도 "매년 음력 O월 O일마다"로 추가됨(부모님 생신 등 극소수
        용도). 한 번 더 누르면 다시 양력 기간 입력으로 돌아오고 숫자는 다시
        0으로 리셋됨).
4) 실행: 오늘 탭에서 오늘 근무 + 오늘 할 것들을 한눈에 보고 체크.

## 반복의 3층 구조 (내부 엔진 개념)
- 층1 달력 반복: 요일·날짜 기준 (매주 화, 매월 15일)
- 층2 주기 반복: 근무 주기 기준 (D번호 엔진). 이 앱만의 차별점.
  ※ 사용자에게 D번호를 직접 노출하지 않는다. "비번날마다" 같은
    사용자 언어로 번역해 보여준다. D는 내부 계산용.
- 층3 간격 반복: "3개월마다, 6주마다" 같은 실제 달력 날짜 간격 (머리, 세차 등).
  구현 완료 — 반복 방식 중 "기간으로 고르기"(rule.type:'interval')로 존재.
  근무 주기(cycleDays)와 무관하게 anchorDate + years/months/days로 계산.
  (예전에 따로 있던 "리마인더" 기능은 이걸로 대체돼서 제거함.)
- 음력 매년 반복: rule.type:'lunar', {lunarMonth, lunarDay}. 부모님 생신처럼
  아주 가끔 쓰는 용도라 가볍게 구현 — 최상단 반복 방식 버튼을 따로 두지 않고
  "기간으로 고르기" 화면 안의 하위 선택("기간"/"음력")으로 넣어둠(promoteMode는
  'interval' 그대로, promoteIntervalSubMode만 'lunar'로 바뀜 — 저장되는 rule.type은
  'lunar'). 매년 그 음력 월/일의 "평달" 발생에만 맞춤(윤달 여부는
  무시 — 그래야 매년 정확히 한 번씩만 걸림). 그 달이 그 해엔 29일까지밖에 없어서
  30일이 존재하지 않으면 그 해는 자동으로 건너뜀. 한국천문연구원(KASI) 기준
  음력 변환 데이터(1000~2050년)를 이용 — 날짜 상세 화면의 음력 표시 기능과
  같은 변환 엔진(solarToLunar/lunarToSolar) 공유.

## 보류/제외 (지금 안 만듦)
- 스킵(못 한 것 표시) 기능: 영구 제외. 다시 만들지 말 것.
- 회고·통계 대시보드·월간 리포트: 영구 제외(앱 정체성 밖).

## 근무유형별 알람 (구현 완료, 네이티브 앱 전용)
- 예전엔 "PWA는 앱/브라우저를 닫으면 알람이 안 울려서 자체 구현 금지"였는데,
  Capacitor로 네이티브 안드로이드 앱이 생기면서 그 제약이 없어짐 — 지금은
  진짜 자체 알람(로컬 알림, 앱이 꺼져 있어도 울림)으로 구현돼 있음.
  **웹(브라우저/GitHub Pages) 버전에서는 여전히 안 울림** — 설치된 안드로이드
  앱에서만 동작(isNativeApp() 체크로 웹에서는 안내 문구만 보여주고 스킵).
- 설정 탭 "근무유형별 알람"(sd-alarms, renderShiftAlarmSettings)에서 근무유형별로
  켜기/끄기 + 시각(HTML time input) 설정. state.shiftAlarms = { [근무이름]:
  {enabled, time:"HH:MM"} }. 저장은 카테고리·별표처럼 바뀔 때마다 즉시(확인
  버튼 없음).
- scheduleShiftAlarms()가 앱을 켤 때(초기화 시점)와 알람 설정을 바꿀 때마다
  호출됨 — 오늘부터 14일치 근무를 계산해서(getEffectiveShiftName 재사용,
  날짜 예외·D번호 예외 다 반영됨), 알람 켜진 근무유형인 날마다 그 시각에
  Capacitor Local Notifications로 예약. id는 그 날짜(YYYYMMDD)를 그대로 씀 —
  하루에 알람 하나뿐이라 날짜 자체가 고유 id. 다시 예약할 때 state.
  _scheduledAlarmIds(직전에 우리가 예약한 id 목록, state에 저장해둠)를 먼저
  취소하고 새로 깜 — 플러그인이 돌려주는 예약 목록에 의존하지 않고 우리가
  직접 추적하는 방식이라 더 확실함.
  알림 채널은 앱 초기화 시 한 번 생성(id: 'shift-alarm', importance:5로 헤드업+
  소리). 권한 요청(ensureAlarmPermission)은 예약 직전에 매번 확인 — 이미
  허용돼 있으면 다시 안 물어봄.
  **한계(1차 버전, 실사용 후 보완 예정)**: SCHEDULE_EXACT_ALARM 권한 없이
  기본 예약 방식만 씀 — Doze 모드에서 몇 분 정도 늦게 울릴 수 있음(칼같이
  정확한 시각이 필요하면 나중에 정확한 알람 권한 추가 검토). 화면을 깨우는
  풀스크린 알람 스타일(진짜 알람시계처럼 잠금화면 위로 뜨는 것)도 아직 없음
  — 지금은 일반 알림(소리+진동, 무음모드 무시 안 될 수 있음)까지만 구현.

## 배포
- GitHub Pages. 저장소 routine-app.
- 배포 = git add → commit → push. ("배포해줘"로 통칭)
- 접속: https://gmdwn916-cmd.github.io/routine-app/
- 데이터는 기기 localStorage에만. 코드 배포는 사용자 데이터에 영향 없음.

## 파일 구조
- index.html 단일 파일(HTML/CSS/JS 일체)에 앱의 실제 로직이 전부 있음 — 아래
  android/www는 이 파일을 감싸는 껍데기일 뿐, 로직은 없음. 번들러(webpack 등)는
  안 씀 — 예전엔 "외부 라이브러리 없음"이었는데, 알람 기능 때문에 Capacitor
  다리 스크립트 2개(capacitor.js, local-notifications.js)만 예외로 추가됨(둘 다
  로직 없는 순수 연결 코드, node_modules에서 그대로 복사해온 것). index.html이
  `<script src="capacitor.js">`/`<script src="local-notifications.js">`로 이
  둘을 불러오므로, **이 두 파일은 index.html과 같은 위치(루트)에도, www/ 안에도
  똑같이 있어야 함** — 루트 것은 GitHub Pages 배포용, www/ 것은 안드로이드 앱용.
  새 Capacitor 플러그인을 추가하면 그 플러그인의 node_modules/@capacitor/
  <이름>/dist/plugin.js도 같은 방식으로 루트+www에 복사하고 index.html에 스크립트
  태그를 추가해야 함(빌드 도구가 없어서 수동으로 하는 것 — 공식 "번들러 없이
  쓰기" 방식). 웹(브라우저)에서는 이 스크립트들이 로드는 되지만 Capacitor.
  isNativePlatform()이 false라서 실제로는 아무 것도 안 함.
- Capacitor로 안드로이드 네이티브 앱 껍데기 추가함(위젯·자체 알람 등 PWA로는
  안 되는 기능을 넣기 위한 전환, 2026-07-13):
  - package.json, node_modules/ — Capacitor 패키지 설치용. node_modules는
    .gitignore에 포함(용량 커서 저장소에 안 올림).
  - capacitor.config.json — 앱 이름 "루틴", 패키지ID `com.hyeongju.routineapp`,
    webDir: `www`.
  - www/index.html — 루트의 index.html 복사본. **여기를 직접 고치면 안 됨** —
    항상 루트 index.html을 고친 뒤 `cp index.html www/index.html`로 복사하고
    **반드시 `npx cap sync android`까지 실행해야** android/app/src/main/assets/
    public/index.html(= APK에 실제로 들어가는 파일)에 반영됨.
    **실제로 겪은 실수(2026-07-14)**: `cp`만 하고 `npx cap sync`를 몇 차례
    빼먹은 채로 `./gradlew assembleDebug`만 반복 실행 — Gradle은 www/index.html이
    바뀌어도 assets/public/index.html을 알아서 다시 복사해주지 않아서, 계속 옛날
    코드가 든 APK가 만들어짐. 겉으로 보이는 증상이 "안드로이드 웹뷰 캐시가 새
    코드를 안 받아준다"처럼 보여서 캐시 문제로 오진단하고, 사용자에게 앱 캐시
    삭제→완전 삭제 후 재설치까지 여러 번 헛되이 시켰음(둘 다 소용없었음 — 당연히
    문제는 기기가 아니라 빌드 쪽에 있었으니까). **재발 방지**: 빌드 스크립트를
    `cp index.html www/index.html && npx cap sync android`로 항상 같이 실행하고,
    의심되면 `diff www/index.html android/app/src/main/assets/public/index.html`
    (또는 `unzip -p app-debug.apk assets/public/index.html | grep <최근 고친 코드>`로
    APK 안의 실제 내용)로 반영 여부를 먼저 확인한 뒤에 "캐시 문제"를 의심할 것.
  - android/ — Capacitor가 생성한 안드로이드 네이티브 프로젝트(Android Studio로 엶).
    android/gradle.properties에 `android.overridePathCheck=true` 추가돼 있음
    — 프로젝트 폴더 이름(루틴어플)에 한글이 섞여 있어서 나오는 경고를 끈 것,
    네이티브(C++/NDK) 코드가 없어서 실제로는 문제 없음. 지우면 빌드 실패함.
  - 빌드: `cd android && ./gradlew assembleDebug` (JAVA_HOME을 Android Studio
    내장 JDK로 지정해야 함, 보통 `C:\Program Files\Android\Android Studio\jbr`).
    결과물은 android/app/build/outputs/apk/debug/app-debug.apk.
- 데이터 백업(내보내기/가져오기) 기능은 이 네이티브 전환 때문에 생김: 웹앱(브라우저)과
  네이티브 앱(Capacitor WebView)은 같은 코드를 실행해도 localStorage가 서로 다른
  저장공간이라 자동으로 안 이어짐 — 설정 탭의 "데이터 백업" 화면에서 JSON 파일로
  내보내고 다른 실행 환경(웹/네이티브)에서 가져오기 하면 이어붙일 수 있음. 아래
  데이터 모델 항목 참고.
  - **내보내기는 웹과 네이티브가 서로 다른 방식을 씀**(btn-data-export 클릭 핸들러
    안에서 isNativeApp()으로 분기): 웹은 blob 링크 다운로드(기존 방식, 문제없음).
    네이티브 앱의 웹뷰는 blob 다운로드를 자체 처리하는 기능이 아예 없어서(일반
    브라우저와 다른 부분) 눌러도 조용히 아무 일도 안 일어남 — 그래서 네이티브
    에서는 WidgetBridgePlugin.saveBackupFile()로 안드로이드가 다운로드 폴더에
    직접 파일을 쓰게 함(MediaStore API, 안드로이드 10+; 9 이하는 예전 방식+
    WRITE_EXTERNAL_STORAGE 권한). 이 구분을 다시 하나로 합치려 하지 말 것 —
    네이티브 웹뷰에서 blob 다운로드가 안 되는 건 플랫폼 자체 한계임.
  - MainActivity에서 웹뷰 캐시를 아예 안 쓰게(WebSettings.setCacheMode(LOAD_NO_CACHE))
    설정해둠(방어적 조치, 유지는 하되 없앨 필요 없음). 다만 **"위젯 갱신 실패"가
    계속되던 진짜 원인은 웹뷰 캐시가 아니라 빌드 실수였음** — 위 www/index.html
    항목의 2026-07-14 메모 참고. 뭔가 "고쳤는데도 안 바뀐다"는 증상이 보이면
    캐시부터 의심하지 말고 먼저 APK 안의 실제 코드를 확인할 것.
- 홈 화면 "빠른 할일 추가" 위젯 (네이티브 전용, 2026-07-13): 위젯을 탭하면 작은
  입력창(QuickAddActivity, 다이얼로그 테마 — 화면 전체 안 가리고 살짝 뜸)이
  뜨고, 글자를 넣으면 앱을 열지 않고도 그 자리에서 저장됨.
  - 네이티브 코드(QuickAddWidgetProvider/QuickAddActivity)는 android/app/src/main/
    java/com/hyeongju/routineapp/ 안에 있음. 위젯은 이 웹뷰의 localStorage를
    직접 못 읽고 못 쓰므로(따로인 저장공간이고, 안드로이드 쪽에서 웹뷰 localStorage를
    직접 건드리는 건 공식 지원 방법이 아님), 입력한 글자는 일단 안드로이드
    SharedPreferences("widget_bridge", 키 "pending_inbox_items", JSON 문자열 배열
    — "임시 우편함")에 쌓임.
  - 이 우편함을 읽고 비우는 다리 역할은 직접 작성한 전용 Capacitor 플러그인
    WidgetBridgePlugin.java가 함(local-notifications처럼 npm으로 받아온 게 아니라
    이 프로젝트 전용으로 새로 쓴 것 — getPendingItems/clearPendingItems 두 메서드뿐).
    MainActivity.java의 onCreate에서 registerPlugin(WidgetBridgePlugin.class)로 등록.
  - JS쪽(index.html)의 getWidgetBridge()/syncWidgetInboxItems()가 앱을 켤 때(초기화
    시점)마다 이 플러그인으로 우편함 내용을 가져와 state.inbox에 옮기고 우편함을
    비움. 위젯은 "글자 목록"만 넘길 뿐 inbox 데이터 형식(id/createdAt 등)을 전혀
    모름 — 실제 inbox 항목으로 바꾸는 건 JS쪽에서만 함.
    앱을 새로 켤 때뿐 아니라, document의 visibilitychange 이벤트(백그라운드에서
    포그라운드로 돌아올 때도 웹뷰가 발생시킴)에도 다시 동기화하도록 돼 있어서,
    "앱을 아예 껐다 켜야만 반영되는" 문제 없음 — 새 네이티브 플러그인 없이 표준
    웹 API만으로 해결됨(App 플러그인 안 씀).
  - 위젯 크기는 처음엔 1x1(가장 작음)에 아이콘(+ 모양, ic_widget_add.xml)만 있고
    글씨가 없었는데, 위젯 선택 화면에서 뭔지 알아보기 힘들다는 이유로(아래
    "위젯 선택 화면" 항목 참고) 2026-07-14에 상단에 "할 일 추가" 글씨를 추가함.
    처음엔 글씨+아이콘을 다 넣으려고 2x1(minWidth 110dp)로 키웠었는데, 사용자가
    "1x1로 유지하고 그 작은 칸 안에 글씨·그림을 잘 정리해서 넣어달라"고 다시
    요청해서 1x1(40dp)로 되돌림 — 다시 위젯 크기를 키우는 방향으로 바꾸지 말 것
    (사용자가 명시적으로 1x1 유지를 원함). 그 안에서 글씨·아이콘 배치는 위쪽
    절반(weight=1) 안에 글씨(현재 12sp, gravity="bottom"으로 그 절반의 아래쪽에
    붙임 — "글씨를 조금 내려달라"는 요청 반영)를, 아래쪽 절반(weight=1)에
    아이콘을 두는 정확히 반반 구조로 확정함(처음엔 8sp+아이콘이 weight=1로
    남는 공간 전부를 차지하던 구조였는데, "+ 공간은 아래 절반만" 요청으로
    지금 구조로 바뀜). 레이아웃도 아이콘 하나만 있던
    FrameLayout에서 세로 LinearLayout(글씨 TextView 위 + 아이콘 아래)으로 바뀜 —
    클릭 반응 영역도 아이콘(widget_add_button)에서 루트 전체
    (widget_quick_add_root)로 넓혀서 글씨를 눌러도 입력창이 뜸.
  - QuickAddActivity는 android:taskAffinity=""로 MainActivity와 완전히 다른
    작업(task)으로 뜸(+ 위젯 쪽 Intent에 FLAG_ACTIVITY_NEW_TASK|MULTIPLE_TASK) —
    안 하면 앱이 이미 켜져 있을 때 위젯을 눌렀을 때 입력창과 함께 앱 화면까지
    같이 튀어나오는 문제가 있었음. windowSoftInputMode="stateAlwaysVisible" +
    코드에서 명시적 showSoftInput 호출로 입력창 뜨자마자 키보드도 자동으로 뜸.

## 이번 달 달력 위젯 (네이티브 전용, 읽기 전용, 2026-07-14)
- 홈 화면에서 달을 날짜별 근무색으로 보여주는 위젯(4x4 크기, 리사이즈 가능).
  수정 기능 없음 — 탭하면 앱만 실행됨(단, 하단 좌우 ‹/› 는 위젯 안에서 달만
  넘김, 앱은 안 열림). 목적이 "그날 근무가 뭔지 한눈에 파악"이라 글씨가 큼
  (여백은 그대로 두고 글씨만 키움), 할 일은 안 보여줌.
- **근무 계산은 전부 JS(index.html)가 하고, 네이티브는 그 결과를 그대로
  그리기만 함** — 위젯 1(빠른 할일 추가)과 반대 방향의 데이터 흐름(이번엔 앱→위젯).
  이렇게 나눈 이유: 근무 계산 규칙(로컬 자정 기준, cycleDays/baseDate, 날짜예외>
  D번호예외>기본패턴 우선순위, 요일 시작 설정)이 네이티브에도 따로 있으면 나중에
  둘 중 하나만 고쳐서 어긋날 위험이 있음 — 절대 안드로이드 쪽에 근무 계산 로직을
  새로 만들지 말 것.
  - JS: buildOneMonthGrid(year, month, weekStart)가 renderGrid()와 완전히 같은
    규칙(getEffectiveShiftName/getShiftColor 그대로 재사용)으로 한 달치 42칸
    그리드를 만들고, buildMonthCalendarPayload()가 **[지난달, 이번달, 다음달]
    3개월치를 항상 이 순서·이 개수로** 계산해서 pushMonthCalendarToWidget()이
    WidgetBridge 플러그인의 setMonthCalendarData()로 한 번에 넘김. payload 형태:
    { headers(7개, 요일 시작 반영), satCol/sunCol(토/일이 몇 번째 칸인지 —
    이것도 네이티브가 안 헷갈리게 JS가 알려줌), months: [ {monthLabel, days(42개,
    그 달에 안 속하면 null, 속하면 {date, dayNum, shiftName, color})} × 3 ] }.
  - 네이티브(WidgetBridgePlugin.setMonthCalendarData)는 그 JSON을
    SharedPreferences("widget_bridge", 키 "month_calendar_data")에 그대로 저장하고,
    보여줄 달 위치(키 "month_calendar_display_index")를 항상 1(=배열의 "이번달")로
    되돌린 뒤 MonthCalendarWidgetProvider.refreshAll()로 위젯을 즉시 다시 그림 —
    위젯에서 지난달/다음달로 넘겨봤어도 앱을 열면 오늘 기준으로 리셋됨.
    "오늘" 강조와 토/일 헤더 색칠만 유일하게 네이티브에서 직접 처리(오늘 강조는
    날짜 문자열 비교, 토/일은 JS가 알려준 열 번호를 그대로 씀 — 둘 다 근무
    로직이 아니라 중복 구현 아님). 토=#007AFF(파랑), 일=#FF3B30(빨강), 앱의
    .grid-header-cell.sat/.sun과 같은 색.
  - **이전/다음 달 넘기기(2026-07-14, 스케줄 위젯과 같은 방식으로 재변경)**:
    처음엔 하단 좌우에 눈에 보이는 ‹(nav_prev)/›(nav_next) 텍스트뷰가 있었는데,
    스케줄 위젯(N주) 쪽에서 먼저 "화살표를 없애고 월/일요일 칸 전체를 탭
    영역으로" 바꾼 뒤 사용자가 "달력 위젯도 같은 방식으로"를 요청해서 동일하게
    바꿈 — nav_prev/nav_next 뷰 자체를 레이아웃에서 제거하고, 대신 월요일 쪽
    열(헤더 칸 + 6줄 그리드 전체, 즉 그 열의 header_N + cell_container_N×6개)
    전부에 이전 달 PendingIntent를, 일요일 쪽 열 전부에 다음 달 PendingIntent를
    걸어서 그 세로 줄 전체가 하나의 탭 영역처럼 동작하게 함 — 다시 작은 화살표
    버튼을 만들지 말 것. 어느 열이 월/일요일인지는 스케줄 위젯과 똑같이
    "일요일 바로 다음 칸이 항상 월요일"(mondayCol = (sunCol+1)%7, JS가 넘겨준
    sunCol만 보고 계산)로 구함. 그 열을 탭하면 항상 페이지 이동만 하고 앱을
    열지 않음(의도된 트레이드오프, 스케줄 위젯과 동일).
    앱을 열지 않고 위젯 안에서만 달을 넘김 — MonthCalendarWidgetProvider가 자기
    자신에게 보내는 커스텀 브로드캐스트(ACTION_PREV/ACTION_NEXT, PendingIntent.
    getBroadcast, onReceive에서 가로챔)로 표시 인덱스(0/1/2)만 바꾸고 다시 그림.
    딱 3개월치만 미리 계산해 넘겨받은 상태라 그 범위(지난달~다음달)를 못
    벗어남 — 더 이전/이후 달을 보려면 앱을 열어야 함(이 범위 밖은 근무 계산
    자체가 없어서 위젯 혼자서는 절대 못 만듦).
  - 위젯 레이아웃(widget_month_calendar.xml)은 헤더 1줄 + 6줄×7칸(칸 컨테이너
    id: cell_container_N, 안에 [날짜 숫자(cell_date_N) / 근무이름 작은 배지
    (cell_shift_N)] 세로 2줄, 앱의 달력 탭 칸(cell-date + cell-shift-badge)과
    같은 모양 — 칸 배경은 중립, 배지만 근무색으로 옅게 칠하고 글자도 근무색).
    id는 리소스 이름으로 동적 조회(Resources.getIdentifier), RemoteViewsService
    같은 복잡한 컬렉션 위젯 아님.
  - 위젯 배경/글자색은 res/values/colors.xml + res/values-night/colors.xml로
    시스템 라이트/다크 모드에 맞춰 자동 전환(widget_bg/widget_text_primary/
    widget_text_secondary). "오늘"/토/일 강조색만 항상 고정색.
  - **갱신 시점**: 앱 시작/포그라운드 복귀 시(위젯 1과 동일한 지점) +
    교대 주기 저장, 주 시작 요일 변경, 근무유형 색 변경, 날짜별/D번호 근무 수정,
    여러 날짜 한번에 근무 수정(bulk) — 이 값들이 바뀌는 모든 저장 시점에서
    pushAllWidgets() 호출(달력 위젯 2 + 스케줄 위젯 3을 한 번에 갱신 — 아래 참고).
    **한계(1차 버전)**: 이 시점들 외에는 안 밀어줌 — 예를 들어 달이 넘어가는
    순간(예: 이번 달 말일 근처) 앱을 며칠간 안 열면 위젯이 지난달 그리드를
    계속 보여줄 수 있음. 앱을 한 번이라도 열면 바로 갱신됨.

## N주 스케줄 위젯 (네이티브 전용, 읽기 전용, 2026-07-14, 2026-07-15 그리드로 재설계)
- **처음엔 세로 목록(날짜 나열)으로 만들었다가, 사용자가 다른 앱 위젯 캡처를
  보여주며 "이거랑 비슷하게" 요청해서 요일 그리드 형태로 다시 만듦** — 목록형
  구조를 다시 만들지 말 것. 지금 구조: 요일 헤더(월~일 또는 일~월, weekStart
  따름) + 2주(2줄×7칸) 그리드, 위젯 2(달력)와 같은 칸 구조를 재사용
  (cell = 날짜 숫자 + 근무 배지 + 할 일 최대 3개를 "• "로 시작하는 줄로).
  단, 배지 색·톤은 참고 앱을 그대로 베끼지 않고 **이 앱 고유의 옅은 톤(15%
  불투명도) 스타일 유지**(사용자가 "앱 UI 통일성 유지"를 명시적으로 요청함) —
  진하고 꽉 찬 배지로 바꾸려 하지 말 것.
- 근무 계산·할 일 판단은 위젯 2와 완전히 같은 원칙으로 전부 JS가 끝냄
  (getEffectiveShiftName/getShiftColor + getRepeatTodosForDate/getEventsForDate를
  그대로 재사용 — 새 규칙 없음). buildSchedulePayload()가 계산하고
  pushScheduleToWidget()이 같은 WidgetBridge 플러그인의 setScheduleData()로 넘김
  (새 플러그인 안 만듦). "오늘" 여부(isToday)도 JS가 미리 계산해서 넘김 —
  네이티브는 그 값으로 오늘 칸 전체(sch_cell_N 컨테이너, 날짜 숫자만이 아님)에
  사각 테두리(widget_today_cell_border.xml)를 입힘 — 날짜만 감싸는 원형이었다가
  "그 날짜 전체에 테두리"로 바뀐 것. 근무 배지(sch_shift_N)도 match_parent
  너비로 칸 좌우 꽉 채움(달력 위젯의 "이어진 띠"와 같은 원리).
- payload 형태: { headers(7개), satCol/sunCol(토/일 열 번호, 달력 위젯과 동일한
  이유로 JS가 알려줌), pages: [ {days(14개, 2주치)} × 3 ] } — 각 day는
  {date, dayNum, isToday, shiftName, color, todos(최대 3개, 이미 "• " 접두 붙은
  문자열 배열)}.
- **페이지 넘기기**: 위젯 크기로 1주/2주를 정하던 예전 방식(리사이즈 기반)은
  없앰 — 대신 자기 자신에게 보내는 커스텀 브로드캐스트로 onReceive에서 페이지
  인덱스만 바꾸는 방식으로 2주씩 묶은 페이지를 넘김. **[지난 2주, 이번 2주,
  다음 2주] 3페이지**(SCHEDULE_PAGE_OFFSET_DAYS = [-14, 0, 14], 달력 위젯의
  [지난달,이번달,다음달]과 똑같은 관례 — 배열 인덱스 1이 항상 "이번 2주")를
  미리 계산해 넘겨받음 — 처음엔 앞으로(다음 2주)만 있었는데, "오늘 이전
  날짜도 보고 싶다"는 요청으로 지난 2주 페이지를 추가함. 그 범위 밖은 앱을
  열어야 갱신됨(달력 위젯과 같은 한계). 참고로 보여준 다른 앱은 스와이프로
  넘기는 방식이었지만, RemoteViews에서 스와이프 페이지는 훨씬 복잡해서
  (AdapterViewFlipper+RemoteViewsService 필요) 안 씀. 앱이 새 자료를 보낼
  때마다 페이지 위치는 1(=이번 2주)로 리셋됨.
  - **탭 영역(2026-07-14 최종 확정)**: 처음엔 하단에 눈에 보이는 ‹/› 버튼을
    뒀는데(따로 줄을 차지 → 헤더 줄에 접어넣음 → 그래도 너무 작아서 못 누름),
    사용자가 아예 화살표 표시를 없애고 "월요일 쪽 세로 전체를 누르면 이전,
    일요일 쪽 세로 전체를 누르면 다음"으로 요청해서 지금 구조로 바뀜.
    화살표 글자(sch_nav_prev/sch_nav_next) 뷰 자체를 레이아웃에서 제거하고,
    대신 월/일요일 각각의 헤더 칸(sch_header_N) + 그 요일의 두 줄 칸
    (sch_cell_N, sch_cell_(7+N)) 세 뷰 모두에 같은 PendingIntent를 걸어서
    "세로 전체가 하나의 큰 탭 영역"처럼 보이게 함 — 다시 작은 화살표 버튼을
    만들지 말 것. 어느 열이 월/일요일인지는 weekStart에 따라 바뀌므로(월요일
    시작이면 월=0열·일=6열, 일요일 시작이면 일=0열·월=1열), "일요일 바로
    다음 칸이 항상 월요일"이라는 항등식(mondayCol = (sunCol+1)%7)으로
    네이티브(ScheduleWidgetProvider)가 JS가 넘겨준 sunCol만 보고 계산 —
    이것도 사소한 배치 계산이라 근무/할일 판단 로직과는 다르게 네이티브에
    둬도 되는 예외로 봄. 이 열들은 이제 탭하면 항상 페이지 이동만 하고
    앱을 열지 않음(그 칸의 할 일을 눌러도 마찬가지) — 의도된 트레이드오프.
- 위젯 1·2·3이 늘어나면서, 여러 저장 시점마다 위젯마다 따로 push 호출을 추가하는
  게 번거로워져서 pushAllWidgets() 함수 하나로 묶음(pushMonthCalendarToWidget()
  + pushScheduleToWidget() + pushTodayWidgetToWidget() 순서로 호출) — 앞으로
  위젯이 더 늘어도 이 함수 안에만 추가하면 됨, 각 저장 시점 코드는 안 건드려도 됨.

## 오늘 전체 관리 위젯 (네이티브 전용, 2026-07-14 — 4개 위젯 세트 마지막, 유일하게 "쓰기" 있음)
- 오늘 날짜·요일·근무를 위젯 2·3처럼 크게 보여주고, 그 아래 오늘의 반복 할일 +
  한 번짜리 할 일을 스크롤 가능한 목록으로 보여줌. 목록의 각 줄을 탭하면 위젯
  안에서 바로 완료 체크가 토글됨 — 위젯 1~3은 전부 "읽기 전용"이었는데 이
  위젯만 유일하게 "쓰기"가 있어서 기술적으로 가장 복잡함.
  - **목록 안 줄마다 따로 탭 반응**: 위젯 2·3처럼 칸을 미리 정해진 개수만큼
    그려두는 방식으로는 "개수가 매번 다르고 스크롤도 되는 목록"을 못 만들어서,
    안드로이드의 "컬렉션 위젯" 방식(RemoteViewsService + RemoteViewsFactory,
    ListView)을 이 위젯에서 처음 씀. TodayWidgetService/내부의
    TodayRemoteViewsFactory가 목록 줄을 실제로 채우고, TodayWidgetProvider는
    헤더(날짜·근무)만 그림. 목록 전체에 "눌리면 이런 신호를 보내라"는 공통
    틀(PendingIntentTemplate, ACTION_TOGGLE)을 걸어두고, 각 줄마다
    TodayRemoteViewsFactory가 그 줄의 항목 id·종류·눌렀을 때 될 상태를
    fillInIntent로 붙여서, 탭 시 그 둘이 합쳐져 TodayWidgetProvider.
    handleToggle()이 어떤 항목을 어떻게 바꿀지 알게 됨. 이 템플릿용
    PendingIntent는 다른 위젯들과 달리 FLAG_MUTABLE로 만들어야 함(fillInIntent가
    안에 끼워져야 하므로) — FLAG_IMMUTABLE로 하면 정보가 안 합쳐짐.
  - **위젯→앱 동기화(이 위젯에서 처음 생긴 흐름)**: 줄을 탭하면 ①
    handleToggle()이 저장해둔 오늘 데이터 안의 그 항목 done 값을 그 자리에서
    바로 바꾸고 notifyAppWidgetViewDataChanged로 위젯을 다시 그려서 즉시 체크된
    것처럼 보이게 함(낙관적 갱신) ② 동시에 "이 항목을 이 상태로 만들어라"
    (id/date/type/done)를 안드로이드 SharedPreferences의 임시 보관함
    (today_widget_pending_toggles)에 쌓음 — 위젯 1의 "임시 우편함"과 같은
    다리 역할. **토글이 아니라 최종 상태를 보내는 게 핵심** — 같은 요청이
    실수로 중복 반영돼도 결과가 달라지지 않아 안전함(반대로 "뒤집어라"였으면
    중복 반영 시 원래대로 되돌아가는 사고가 날 수 있음). JS쪽
    syncWidgetTodayToggles()가 앱 시작/포그라운드 복귀 시(위젯 1·2·3과 같은
    지점, syncWidgetInboxItems 바로 옆에서 같이 호출) 이 보관함을 읽어서
    실제 done_log(반복 할일)·ev.done(한 번짜리)에 반영하고 보관함을 비운 뒤,
    pushAllWidgets()로 최신 상태를 위젯들에 다시 보냄. 오늘 탭이 화면에 떠
    있는 상태로 복귀하면 renderToday()도 같이 불러서 화면도 바로 갱신됨.
  - buildTodayWidgetPayload()가 오늘의 반복 할일(getRepeatTodosForDate)과
    한 번짜리 할 일(getEventsForDate)을 합쳐서, 오늘 탭(renderTodayTimeline)과
    완전히 같은 순서(시간대 미정→오전→오후→밤, 그 안에서 timelineOrder)로
    정렬해 넘김 — 위젯 목록 순서가 앱 오늘 탭과 어긋나지 않게 함. 각 항목은
    {id, text, done, icon(카테고리 이모지), type:'repeat'|'once'}.
  - 완료된 항목은 위젯 목록에서 글자에 취소선을 긋고(TextView.setPaintFlags)
    흐린 색으로, 체크 아이콘도 채워진 동그라미(widget_check_on)로 바꿔서
    구분 — 항목을 목록에서 숨기지는 않음(스케줄 위젯의 "완료된 항목은 안
    보이게"와는 다른 성격 — 이 위젯은 체크 자체가 목적이라 방금 체크한 항목이
    바로 사라지면 확인이 안 됨).
  - 위젯에서 새 항목 추가는 없음(그건 위젯 1의 역할) — 이 위젯은 "오늘 보고
    체크"만.

## 위젯 2·3·4 다크/라이트 모드 배경-글자색 어긋남 버그(2026-07-14 수정)
- 증상: 시스템이 라이트 모드인데 위젯 안 날짜 글자가 흰색으로 나와서 흰
  배경 위에 흰 글자가 겹쳐 안 보임(사용자 신고).
- 원인: 위젯 배경(레이아웃 XML의 `android:background="@drawable/widget_background"`,
  안에서 `@color/widget_bg` 참조)은 이 위젯을 실제로 그리는 홈 화면 런처가
  "그 순간 자신의" 다크/라이트 상태로 화면에 그릴 때마다 실시간으로 다시
  해석해서 그림. 반면 글자색(`views.setTextColor(...)`)은 우리 앱이 마지막으로
  push했던 그 순간의 다크/라이트 상태를 이미 확정된 색(RemoteViews에 심어지는
  순간의 리터럴 값)으로 심어서 보내는 것이라, 그 이후 push 없이 시스템
  다크/라이트만 바뀌면 배경은 즉시 새 상태로 바뀌는데 글자색은 예전 상태
  그대로 남아 서로 어긋남(다크 모드일 때 push해서 흰 글자로 심어둔 상태에서,
  앱을 다시 열지 않은 채 시스템만 라이트로 바뀌면 흰 배경 위에 흰 글자만
  남는 사고).
- 고침: 배경도 글자색과 완전히 같은 순간·같은 판단으로 우리가 직접 골라
  심음 — `widget_background_light.xml`/`widget_background_dark.xml`(색이 고정된
  채로 각각 따로 있는 모양, `@color` 참조 없음)을 새로 만들고,
  `context.getResources().getConfiguration().uiMode`로 그 순간의 다크/라이트를
  판단해 `views.setInt(rootId, "setBackgroundResource", ...)`로 배경도 매번
  push할 때 명시적으로 골라 심음(MonthCalendarWidgetProvider/
  ScheduleWidgetProvider/TodayWidgetProvider 셋 다 동일하게 적용). 이러면
  배경과 글자색이 항상 "같은 순간의 판단"으로 맞아 있음이 보장됨 — 다만
  시스템 테마가 바뀐 뒤 앱을 아직 안 열었으면 여전히 예전 테마 그대로
  보일 수 있음(둘 다 예전 상태로 일치하니 최소한 안 보이는 사고는 없음).
  더 완벽하게 실시간으로 맞추려면 시스템 테마 변경 브로드캐스트를 들어야
  하는데, 위젯 수신자(BroadcastReceiver)는 매니페스트만으로는 그 브로드캐스트를
  안정적으로 못 받아서(최신 안드로이드의 암시적 브로드캐스트 제한) 이번엔
  그 범위까지는 안 하고, "앱을 열 때마다 항상 다시 맞춰짐" 정도로 충분하다고
  판단함. **(2026-07-14 갱신)** 위젯 1(빠른 할일 추가)은 처음엔 날짜/글자색이
  없고 고정 파란색 아이콘뿐이라 이 버그 대상이 아니었는데, 같은 날 상단에
  "할 일 추가" 글씨(라벨)가 추가되면서 이 위젯도 똑같은 어긋남 버그 대상이 됨 —
  그래서 QuickAddWidgetProvider에도 나머지 셋과 동일한 방식(isDark 판단 +
  widget_background_light/dark 명시적 선택)을 똑같이 적용해둠.

## 위젯 선택 화면(홈 화면에서 위젯 추가할 때 뜨는 목록) 표시 (2026-07-14)
- 예전엔 4개 위젯 전부 안드로이드 기본값(앱 이름 "루틴"만 반복 표시, 미리보기
  없음)이라 어느 게 어느 위젯인지 위젯 선택 화면에서 구분이 안 됐음 — 사용자가
  "각 위젯의 미리보기 모습을 보여주고, 제목을 붙여줘"로 요청해서 고침.
  - **제목**: 각 위젯을 등록하는 AndroidManifest.xml의 `<receiver>` 태그에
    `android:label`을 달아줌(안드로이드가 위젯 선택 화면에 보여주는 이름은 앱
    자체 라벨이 아니라 이 리시버의 label을 씀) — QuickAddWidgetProvider="할 일
    추가", MonthCalendarWidgetProvider="달력", ScheduleWidgetProvider="스케줄",
    TodayWidgetProvider="오늘 할일". **ScheduleWidgetProvider는 사용자가 말한
    "일정" 대신 "스케줄"로 붙임** — 이 프로젝트는 "일정"이라는 표현 자체를 안
    쓰기로 정해뒀어서(용어 통일 규칙 참고) 위젯 이름에도 그 규칙을 그대로
    지킴. "일정"이 아니라 다른 표현이 필요하면 그때 다시 정할 것.
  - **미리보기**: 각 위젯의 xml/widget_*_info.xml에 `android:previewLayout`을
    추가해서, 위젯이 실제로 쓰는 초기 레이아웃(@layout/widget_*)을 그대로
    미리보기로 재사용함 — 가짜 스크린샷 이미지를 따로 만들지 않고 진짜
    레이아웃을 그대로 보여주는 방식이라 나중에 위젯 모양이 바뀌어도 미리보기가
    저절로 같이 바뀜(수동으로 이미지 다시 만들 필요 없음). **한계**: 이 속성은
    안드로이드 12(API 31) 이상에서만 동작 — 그보다 낮은 버전 기기에서는 위젯
    선택 화면에 미리보기 없이 기본 아이콘만 나올 수 있음(제목은 그 기기에서도
    정상 표시됨). 더 낮은 버전까지 미리보기가 꼭 필요하면 `android:previewImage`
    로 정적 이미지를 따로 만들어 추가하는 방법이 있음 — 지금은 안 만들어둠.

## 용어 (통일 — 혼동 금지)
- 할 일 = state.events[] 전체. 반복이 꺼져 있으면 한 번짜리(특정 날짜),
  반복이 켜져 있으면 반복 할일(근무/요일/직접 규칙으로 매번 계산해서 나타남).
  둘 다 같은 객체(할 일 + repeat 여부)일 뿐, 서로 다른 종류의 물건이 아니다.
- 반복 할일 = 할 일 중 repeat 규칙이 있는 것 (예전 "루틴". 하단 탭 이름도 "반복 할일").
  더 이상 날짜별로 복사돼 있지 않고, 하나의 객체 + 규칙으로 존재하며
  화면에 뿌릴 때만 그 날짜에 해당하는지 계산한다.
- 미배치 = 아직 안 놓은 것 (인박스)
- "일정"이라는 표현은 쓰지 않는다.
- "리마인더"라는 이름의 별도 기능(설정 탭 안, 간격/날짜형)은 폐기됨. 반복 할일의
  "기간으로 고르기"(층3)로 대체됐으니 다시 만들지 말 것.
- "트리거"(항목 묶어 보여주던 태그) 기능은 폐기됨. 다시 만들지 말 것.

## 작업 방식 (반드시 지킬 것)
1. 코드 수정 전 항상 계획 먼저 제시, 사용자 확인 후에만 수정.
   - 계획 설명은 프로그래밍 용어를 빼고 쉬운 말로 할 것.
   - 계획을 승인받으면 그 뒤로는 세부 진행마다 다시 묻지 말고
     배포까지 끝까지 처리할 것(권한 확인 재질문 금지).
2. 수정 파일은 명시된 것만.
3. 데이터 구조 변경 시 기존 localStorage 보존, 마이그레이션 필수.
4. 수정 후 셀프체크하고 한국어로 요약 보고.
5. 모든 응답 한국어.
6. 새 기능·화면은 '핵심 시스템' 흐름과 '경계 기준'에 맞는지 먼저 검토.
   어긋나면 만들기 전에 사용자에게 알릴 것.

## 데이터 모델 (localStorage key: shiftRoutine)
- 설정 탭 "데이터 백업"(sd-data)에서 이 localStorage 전체를 JSON 파일로 내보내기/
  가져오기 가능(btn-data-export/btn-data-import). 가져오기는 확인창(confirm) 이후
  localStorage를 통째로 덮어쓰고 location.reload() — 마이그레이션은 그 뒤 초기화
  과정(migrateState)에서 평소처럼 자동으로 돎.
- cycleDays, baseDate("YYYY-MM-DD"), days[]{dayIndex, shiftType:{name}, items[]}
  ※ days[].items는 더 이상 쓰지 않음(항상 빈 배열). 과거 루틴 데이터 마이그레이션
    이후의 흔적일 뿐이니 이 필드 기준으로 새 기능을 만들지 말 것.
- events[]: 할 일 전체(한 번짜리 + 반복 할일 모두 여기 하나에 있음)
  - 공통: { id, text, category }
  - 한 번짜리(repeat 없음): { date, endDate, done }
  - 반복 할일(repeat 있음): { repeat: rule } — date/endDate/done은 안 씀,
    대신 done_log를 (date, id) 기준으로 찾아서 그날 완료 여부 판단.
  - rule 종류: {type:'shift', groups:[{shiftNames:[...], offset, weekdays?}, ...]}
               ("근무·요일로 고르기". groups는 (근무+요일) 조건 묶음 여러 개를
                OR로 합친 것 — 저장 형식 자체는 그대로지만 편집 화면은 표
                형태(promote-shift-matrix)로 완전히 바뀜: 근무×요일 칸을
                직접 켜고 끄고, "조건 추가" 같은 단계도 화면에 규칙을 설명하는
                문구도 없음. 저장할 때 collapseGridSelectionToGroups()가
                근무(행) 하나당 묶음 하나로 압축 — 그 근무의 요일 7칸이 전부
                켜져 있으면 weekdays 없이("이 근무 날마다"), 아니면 켜진
                요일만 담음. 편집하러 열 때는 expandGroupsToGridSelection()이
                반대로 묶음들을 표의 켜진 칸으로 풀어줌(shiftNames/weekdays가
                비어 있던 예전 묶음은 그 축 전체를 켜서 표현). 예전 통짜 형식
                {shiftNames, offset, weekdays}(묶음 1개짜리와 동일)도 계속
                읽힘 — 별도 마이그레이션 없이 computeIndicesForRule/
                describeRepeatRule이 groups 없으면 그 자리에서 묶음 1개로
                감싸서 처리.
                offset("이 근무 다음날"): 화면에서 고르는 UI가 없어서 새로
                만드는 묶음은 항상 offset:0("이 날"). 예전에 offset:1(다음날)로
                저장된 묶음은 계속 그 값 그대로 정상 계산·표시되지만(하위 호환),
                표에서 다시 편집해서 저장하면 offset:0으로 바뀜 — 의도된
                동작, 다시 만들지 말 것)
              {type:'weekday', weekdays:[...]}  (예전 "요일로 고르기" 전용 모드였음 —
               버튼은 없어졌지만 이 형식으로 저장된 예전 데이터를 계속 읽기
               위해 computeIndicesForRule/describeRepeatRule에 남겨둠. 그런
               항목을 열어서 수정하면 그 자리에서 shift 묶음(근무 없이 요일만)
               형식으로 자동 전환됨)
              {type:'interval', years, months, days, anchorDate}  (기간으로 고르기.
               근무 주기와 무관하게 실제 달력 날짜로 계산)
              {type:'lunar', lunarMonth, lunarDay}  (기간 안의 "음력" 하위 선택.
               매년 그 음력 월/일의 평달 발생에만 맞춤 — 윤달 무시)
  - shift/weekday는 computeIndicesForRule(rule)로 그 규칙에 맞는 D인덱스 목록을 계산.
    interval은 D인덱스 개념이 없고 isIntervalRuleMatch(rule, dateStr)로 anchorDate부터의
    간격을 직접 날짜 연산해서 판단(cycleDays/baseDate 미설정이어도 동작).
    lunar도 D인덱스 없이 isLunarRuleMatch(rule, dateStr)로 solarToLunar 변환 결과를
    직접 비교해서 판단.
    getRepeatTodosForDate(dateStr)가 이 셋을 분기해서 그 날짜의 반복 할일 목록을 조회.
  - 반복 규칙 고르는 화면(기간[구석의 "음력" 토글로 양력/음력 전환]/근무·요일)은
    openRepeatEditor() 하나로 공용
    (새로 만들기·기존 할일을 반복으로 전환·기존 반복 할일 편집 모두 이 함수).
    삭제/확인/취소 버튼이 없음(의도적으로 없앤 것 — 다시 만들지 말 것):
    - 저장: 화면에서 뭔가 바뀔 때마다(텍스트 입력, 카테고리 선택, 근무·요일
      칸 클릭, 기간/음력 스피너를 놓을 때, 모드 전환) syncPromoteEditorToState()가
      바로 저장. 새로 만들기 중엔 텍스트가 비어 있거나 규칙을 아직 하나도
      못 만들 상태(예: 모드 자체를 안 골랐거나 칸을 하나도 안 켬)면 저장을
      보류하고 조용히 기다림 — 텍스트+유효한 규칙이 갖춰지는 순간 그때
      state.events에 항목이 생김. 닫기는 배경(바깥) 탭으로만.
    - 삭제: 이 화면 안에 버튼 없음. "반복 할일" 탭 목록(renderRoutine)에서
      한 번짜리/오늘 탭 할 일과 똑같이 왼쪽으로 스와이프해서 지움
      (wrapWithSwipeToDelete 재사용). 여기서 스와이프는 그 반복 항목을
      통째로 지우는 것 — 오늘 탭/날짜 상세에서 스와이프하면 그 날짜만
      제외(excludedDates)하는 것과는 다르니 혼동 금지(아래 excludedDates
      설명 참고, 예전에 실제로 헷갈려서 버그 신고된 적 있음).
  - "반복 할일" 탭 목록(renderRoutine)은 카테고리가 아니라 반복 방식으로 섹션을
    나눠 보여줌: 근무·요일(shift/weekday) → 기간(interval) 순. 음력(lunar)은
    화면(반복 편집 화면)에서도 "기간" 안의 하위 선택이므로 이 탭 정리에서도
    따로 안 빼고 "기간" 섹션에 같이 묶임. 이 탭은 "반복이 어떻게 걸리는지"
    확인·수정하는 용도라 방식별 묶음이 맞음(카테고리는 각 줄 아이콘/색으로
    이미 표시되니 그걸로 또 묶지 않음). repeatGroupKey(rule)이 분류를 담당.
  - excludedDates?: [dateStr,...] — 오늘 탭/날짜 상세에서 반복 할일을 왼쪽으로
    쓸어 삭제하면 그 날짜만 여기 추가됨(반복 할일 자체는 안 지워짐).
    반복 할일 전체 삭제는 반드시 "반복 할일" 탭에서만(openRepeatEditor의
    삭제 버튼). 이 구분 헷갈리지 말 것 — 실제로 헷갈려서 버그 신고된 적 있음.
  - 화면에 그릴 때는 반드시 buildTodoRow()/renderTodoList() 공용 함수로 그릴 것.
    반복 할일과 한 번짜리 할 일은 겉모습(체크 버튼·카테고리 점·인라인 수정·
    스와이프 삭제)이 완전히 같아야 함 — 별도 마크업/스타일을 새로 만들지 말 것.
    (예전엔 today-event-item/dd-event-item으로 따로 그렸다가 통합함.)
    buildTodoRow의 5번째 인자(opts.dragHandle)는 오늘 탭 타임라인에서만 켜서
    드래그 손잡이를 붙이는 용도 — 반복/한 번짜리 사이 차별이 아니라 화면(오늘
    탭 vs 날짜상세) 차이이므로 이 통일 규칙에 어긋나지 않음.
  - timeSlot?: 'morning'|'afternoon'|'night' — 오늘 탭 타임라인 구역. 없으면
    "미정"으로 취급(값이 'unset'으로 저장되지는 않음, 그냥 필드 자체가 없는
    상태 = 미정). 카테고리·important처럼 그 할 일 객체 자체에 저장되는 정보라
    반복 할일은 매번 같은 구역에 나타남.
  - timelineOrder?: number — 같은 timeSlot(미정 포함) 안에서의 순서. 없으면 0
    취급. 오늘 탭에서 손잡이(.item-drag-handle, "≡")를 꾹 눌러 드래그하면
    setupTimelineDrag()가 놓인 구역의 timeSlot과, 그 구역 안 최종 DOM 순서
    기준으로 형제 항목들의 timelineOrder를 전부 10 간격으로 재계산해서 저장.
    이 드래그는 스와이프 삭제와 같은 줄(li)을 쓰지만 손잡이에서 시작한 터치만
    반응하도록 e.stopPropagation()으로 완전히 분리해뒀음 — 손잡이를 없애거나
    다른 요소와 합치면 두 제스처가 서로 간섭하니 주의.
- inbox[]: 미배치 { id, text, createdAt, targetMonth? }
  - targetMonth?: "YYYY-MM" — 연간 탭에서 월 박스로 드래그해서 "대략 이 달에"로
    태그해 둔 것. 있어도 여전히 미배치 상태(세 번째 상태 아님, 위 핵심 시스템
    참고). setupInboxItemDrag()가 롱프레스 드래그를, renderInboxList()가
    현재 보고 있는 달(calYear/calMonth, tab-month 활성 시)에 맞는 targetMonth
    항목만 상단에 별도 표시하는 로직을 담당.
  (반복 배치 옵션은 아직 없음, 범위 밖으로 보류 중)
- shiftOverrides: 날짜 예외{date,shiftName} / D번호 예외{dayIndex,shiftName}
- shiftColors { 근무이름: 색 }, weekStart
- done_log[]: { date, id } — 반복 할일의 그 날짜 완료 기록.
  (예전엔 text로 저장해서 이름 바꾸면 기록이 끊겼는데, id로 바꿔서 해결됨)
  스킵 기능은 제거됨(state.skips는 더 이상 안 씀).
- teams[] { name, offsetDays }
- routinesMigratedV1: true — 예전 루틴(day.items)을 반복 할일로 1회 변환했다는 표시.
  이 값이 있으면 마이그레이션을 다시 돌리지 않는다(중복 생성 방지).

## 핵심 로직 (건드릴 때 주의)
- 날짜는 반드시 로컬 자정 기준. parseLocalDate() 사용.
  new Date("YYYY-MM-DD") 직접 파싱 금지(UTC 밀림).
  diff = Math.round((todayMid - baseMid)/86400000)
  dayIndex = ((diff % cycleDays) + cycleDays) % cycleDays + 1
- 근무 표시 우선순위: 날짜 예외 > D번호 예외 > 기본 패턴.
  설정탭 기본 근무 패턴은 어떤 수정으로도 바뀌지 않는다.
- 테마: data-theme 명시(light/dark) > @media 시스템 감지.
  @media 규칙은 :root:not([data-theme])로 한정.
- 카테고리는 설정 탭에서 사용자가 직접 추가·수정(이름/아이콘)·삭제 가능
  (state.categories — CATEGORIES_DEFAULT는 최초 1회 씨앗 데이터일 뿐, 이후엔
  안 씀). 이름·아이콘은 입력칸 하나에 같이 입력(예: "🥋 운동") —
  splitCategoryInput()이 맨 앞 이모지 1개를 아이콘으로, 나머지를 이름으로
  자동 분리. 따로 나뉜 입력칸을 두지 않음.
  - CATEGORIES_DEFAULT 8개 색은 근무유형 기본색(SHIFT_COLORS: 파랑·보라·초록·
    주황)과 안 겹치게 고른 팔레트(브라운·시안·틸·노랑·민트·보라(마젠타 계열)·
    핑크·회색). 새 카테고리 색을 추가/변경할 때도 이 4개 근무색과 안 겹치게
    고를 것 — 달력에 근무 배지와 카테고리 점이 같이 보여서 겹치면 헷갈림.
    "기타"의 회색만 휴무 색과 겹치는데 이건 의도된 것(둘 다 "특별히 없음"
    중립색이라 안 헷갈림). 색상 선택 프리셋도 근무유형용(SHIFT_COLOR_PRESETS)과
    카테고리용(CATEGORY_COLOR_PRESETS)을 따로 둠 — 카테고리 프리셋은
    CATEGORIES_DEFAULT와 정확히 같은 8색이라 설정 화면에서 항상 하나가
    "선택됨"으로 표시됨.
  - "기타"(id:'etc')는 항상 맨 끝에 회색(#8e8e93)으로 고정 — 수정·삭제 불가
    (설정 화면에 이름만 "고정" 표시로 보여주고 색상/삭제 UI 자체가 없음).
    카테고리가 없거나 잘못된 항목은 늘 이 "기타"로 폴백됨(getCat).
    migrateState()가 매번 "기타"가 마지막에 정확히 이 모양인지 검사해서
    아니면 되돌림(순서가 밀렸거나 예전 버전에서 색이 바뀌었어도 자동 복구) —
    그래서 일반 카테고리는 몇 개를 지우든 항상 최소 1개("기타")는 남음.
  - getCategories()/getCat(id)로 조회 — getCat은 없는 id면 CAT_FALLBACK으로
    폴백(카테고리를 삭제해도 그 카테고리를 쓰던 할 일이 깨지지 않게).
  근무 색·디자인 토큰은 여전히 코드 상단 상수로 관리(사용자 수정 불가).
- 아이콘 체계: 앱 고정 아이콘(하단 탭바, 설정 메뉴 6개, "다른 팀 근무 확인"/
  "미배치"/"달력보기" 버튼)은 전부 선 아이콘 SVG로 통일(viewBox 0 0 24 24,
  stroke:currentColor, fill:none, stroke-width:1.8, round cap/join — 하단
  탭바 스타일 그대로). 설정 메뉴용은 SETTINGS_ICONS 상수에 모아둠. 반대로
  **카테고리 아이콘은 이모지 그대로 유지** — 사용자가 직접 아무 이모지나
  골라 쓰는 기능이라(예: "🥋 운동") 고정 아이콘 세트로 바꿀 수 없음. 새 고정
  버튼을 추가할 때 이모지를 쓰지 말고 이 SVG 스타일을 따를 것.
- 달력 칸 렌더링은 화면 간 공유(달력 탭/팀 근무/복수선택 통일 유지).

## 화면 구성 (하단 탭 5개)
- 연간 / 달력(구 전체보기) / 반복 할일(구 루틴) / 오늘 / 설정 (+ 미배치 버튼은 전 화면 공통)
- 연간 탭: 1~3/4~6/7~9/10~12월을 4줄×3칸으로, 각 달 상자를 누르면 그 달의
  달력 탭으로 이동. 달 상자엔 그 달에 "★ 중요 표시"된 할 일(반복이든
  한 번짜리든 상관없음, getImportantEventsForMonth)을 그 달에서 처음
  나타나는 날짜 순으로 최대 3개까지 보여줌. 생일처럼 매년 반복되는 것도
  중요 표시만 해두면 매년 뜸 — 반복이라고 걸러내지 않고 사용자가 별표로
  알아서 정하게 함(예전엔 반복 할일을 통째로 제외했었는데 이 원칙에 안 맞아
  없앰).
- ★ 중요 표시는 buildTodoRow()에서 반복 할일·한 번짜리 할 일 모두에 똑같이
  보임(item.important). 반복으로 전환할 때도 그대로 이어받는다.
- 날짜 상세는 "반복 할일 + 한 번짜리 할 일"을 한 목록으로 합쳐서 보여줌
  (따로 섹션 나누지 않음, renderTodoList).
- 오늘 탭은 renderTodoList 대신 renderTodayTimeline으로 그림 — 미정(맨 위)·
  오전·오후(~18시)·밤 4구역으로 나눠서 보여줌(아래 timeSlot 설명 참고).
  이 구분은 오늘 탭에만 적용, 날짜 상세는 안 바뀜.

## 안 만들 것 (포지션 유지)
- 범용 습관 트래커·만능 투두·일반 근무자용 확장 금지.
  '교대근무자'라는 틈에 집중. 넓히려는 유혹을 참는다.
- 웹(PWA) 버전은 자체 알람 구현 안 함(브라우저 한계로 불가능). 네이티브 앱
  버전은 위 "근무유형별 알람" 섹션 참고 — 이미 구현돼 있음.
