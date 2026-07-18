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
   - 하단에 항상 떠 있는 전역 빠른 추가 바(quick-bar) 하나로 처리
     (submitQuickAdd가 오늘 탭이면 오늘 날짜, 날짜 상세가 열려있으면 그
     날짜(detailDateStr)로 바로 배치, 둘 다 아니면 미배치로 보냄). 날짜 상세
     화면 안에 이것과 별개인 자체 입력창을 다시 만들지 말 것 — 전역 바
     하나로 통일된 상태를 유지할 것.
   - **쉼표(,)로 연속 입력(2026-07-18 추가)**: 이 입력칸에 쉼표를 치는
     순간 그때까지 쓴 글자를 바로 addQuickAddItem()으로 추가하고 입력칸은
     빈 채로 유지(포커스도 그대로) — "빨래,청소,운동"처럼 이어 쳐서 여러
     개를 한 번에 넣을 수 있음. `input` 이벤트에서 값에 쉼표가 있는지
     검사해 앞부분들을 전부 추가하고 마지막(아직 안 끝난) 조각만 입력칸에
     남김(붙여넣기로 쉼표가 여러 개 한 번에 들어와도 전부 처리). 위젯 쪽도
     동일하게 지원(아래 "위젯 공통 규칙"의 쉼표 항목 참고) — 앱 화면과
     위젯을 항상 짝 맞춰 관리하기로 한 원칙(맨 아래 "작업 방식" 참고)에
     따라 같이 구현함.
   - 날짜를 모르면 '미배치(인박스)'로 들어간다.
2) 미배치(인박스): 화면 어디서든 항상 떠 있는 버튼(미처리 개수 배지).
   - 누르면 미배치 리스트 → 항목을 골라 배치.
   - 항목 상태는 딱 둘: 미배치 / 배치됨. "월 태그"(아래)는 새로운 세 번째
     상태가 아니라 미배치 상태 안의 부가 정보일 뿐 — 미배치 배지 개수에
     계속 포함됨. 진짜 세 번째 상태를 만들지 말 것.
   - **대략 배치(월 태그)**: 몇 날짜에 할지는 아직 모르지만 "대충 몇 월에
     하면 되겠다"는 감만 있을 때. 연간 탭에서 미배치 항목을 꾹 눌러
     드래그하면(길게 누르는 순간 미배치 창이 사라지고 뒤의 연간 화면이
     드러남) 원하는 달 박스에 놓아서 그 달로 태그(inbox item에
     targetMonth:"YYYY-MM" 저장). 월 박스 자체에는 아무 표시 없음(지저분해
     지지 않도록). 태그된 항목은 평소 미배치 목록에는 안 보이고, 그 달의
     달력(전체보기) 화면을 보면서 미배치를 열었을 때만 위쪽에 따로 나타남 —
     거기서 다시 꾹 눌러 드래그해서 날짜 칸에 놓으면 확정 배치(targetMonth
     없어짐).
   - **탭으로 배치**: 항목을 짧게 탭하면 미배치 창이 닫히면서 지금 보고
     있던 화면이 그대로 배치 대상이 됨(startInboxPlacement) — **연간 탭을
     보고 있었으면** 그 화면에서 월 박스 탭 = 월 태그(위 "대략 배치"와 결과
     동일, 드래그 대신 탭 두 번). **그 밖의 탭(달력 탭 포함)을 보고
     있었으면** 달력 탭으로 전환되고 날짜 칸 탭 = 확정 배치. 화면 위쪽에
     얇은 안내 띠(#placement-hint-bar, "넣을 달/날짜를 선택하세요" + 취소
     버튼)만 뜸 — **칸마다 테두리 강조는 넣지 말 것**(한 번 넣었다가 "테두리는
     안 보이게" 요청으로 뺐음, 안내 띠 문구로 충분하다고 판단됨). 취소 버튼
     또는 미배치 버튼 재클릭으로 배치 취소(cancelPlacementMode, openInbox
     안에서 항상 먼저 호출). 미배치 목록이 펼쳐진 채로 다른 탭으로
     넘어가면(하단 탭 직접 클릭이든 위젯으로 진입이든) switchTab()이 항상
     같이 닫음(closeInbox — 날짜 상세를 항상 같이 닫는 것과 동일한 원칙).
     배치 도중 연도/월을 넘겨도(yearViewYear, calYear/calMonth) 배치 모드는
     유지됨. 드래그 방식(연간 탭 한정)도 그대로 남아있음 — 둘 다 같은 결과로
     이어지는 서로 다른 손짓일 뿐, 하나가 다른 하나를 대체하지 않음.
   - **배치 해제**: 이미 배치된(날짜 있는) 한 번짜리 할 일도, 줄의 손잡이(≡)를
     꾹 눌러 드래그해서 하단 미배치 버튼 위에 놓으면 미배치로 돌아감
     (unplaceEventToInbox — 기존 이벤트를 지우고 새 미배치 항목으로 만듦, id는
     새로 부여 — placeInboxItem이 반대 방향일 때도 항상 새 id를 주는 것과
     대칭). 오늘 탭(setupTimelineDrag)과 날짜 상세(setupUnplaceDrag) 둘 다
     지원. **반복 할일은 대상 아님**(특정 날짜에 배치된 게 아니라 매번 규칙으로
     계산되는 것이라 "날짜 지정 해제" 개념 자체가 성립 안 함) — 오늘 탭에서
     반복 할일 손잡이를 미배치 버튼에 올려도 무반응, 날짜 상세의 반복 할일
     줄에는 이 손잡이 자체가 없음.
   - **내용 수정**: 미배치 목록에서 항목을 꾹 눌렀다가(움직이지 않고) 떼면
     그 자리에서 바로 인라인 수정(startInlineTextEdit 재사용). 같은 "꾹
     누르기" 제스처 안에서 셋으로 갈림: 짧은 탭(500ms 안에 뗌)→배치 모드,
     꾹 눌렀다 움직이지 않고 뗌→내용 수정, 꾹 누른 채 이동→월/날짜로
     드래그. setupInboxItemDrag 하나의 상태 기계로 처리(움직임이 있어야만
     드래그로 확정되고, 그 전까지는 미배치 창을 안 닫은 채 기다림 — 수정할
     항목이 화면에서 사라지지 않도록).
   - **레이아웃 주의**: #day-detail-overlay는 화면 전체(inset:0)를 덮으면
     안 됨 — 하단 전역 바(#bottom-fixed, 빠른 추가 입력칸 + 미배치 버튼)가
     가려져서 "날짜 상세에서 바로 입력"도 "미배치로 드래그"도 안 됨. bottom을
     `var(--bottom-fixed-height)`만큼 띄워서 하단 바 자리를 항상 비워둘 것.
   - **하단 바-날짜상세 사이 틈 버그(해결됨)**: 날짜 상세 화면과 하단 빠른
     입력바 사이로 달력 탭이 비쳐 보이고 눌리기까지 하던 버그의 진짜 원인은
     키보드가 아니라, "하단 바 높이는 항상 156px"라는 숫자를 코드 세
     군데(#day-detail-overlay의 bottom, .tab-content의 아래 패딩,
     #day-detail-panel의 max-height 계산)에 똑같이 못박아둔 것 — 실제로는
     기기의 하단 안전영역(제스처 내비게이션 여백 등)에 따라 하단 바의 진짜
     높이가 156px보다 커질 수 있어서 그 차이만큼 항상 틈이 남아 있었음
     (adjustResize나 visualViewport 같은 키보드 보정을 먼저 시도했지만 근본
     원인이 아니라서 효과 없었음). 고침: `ResizeObserver`로 #bottom-fixed의
     실제 렌더링 높이를 재서 CSS 변수 `--bottom-fixed-height`에 담고(:root
     기본값 156px는 JS가 못 채웠을 최초 한 순간 대비), 세 곳 다 이 변수를
     씀. **다시 156px 같은 숫자를 하드코딩하지 말 것** — 하단 바 높이가
     바뀔 만한 수정을 해도 이 변수 덕분에 자동으로 따라감. 비슷한 "하단
     바와 다른 요소 사이 틈" 증상이 보이면 `--bottom-fixed-height`가 실제
     `#bottom-fixed`의 `offsetHeight`와 같은지부터 확인할 것.
   - **닫기 방식**: 날짜 상세 패널 안에 닫기 버튼 없음 — 패널 바깥의 어둡게
     깔린 배경(오버레이 자기 자신, e.target===this)을 탭하거나 하단 탭 중
     아무거나 눌러 다른 화면으로 넘어가면 자동으로 닫힘(switchTab() 맨 앞에서
     closeDayDetail() 항상 호출). 패널 안쪽 탭은 안 닫힘. 닫기 버튼을 다시
     만들지 말 것.
3) 배치: 항목당 남은 질문은 하나만(넣기의 OX가 첫 갈림길이므로).
   - 반복 X → "언제?" : 근무가 보이는 달력에서 날짜 하나 지정.
   - 반복 O → "어떤 날마다?" : 버튼 2개만 제공("기간" / "근무·요일") — 예전에
     따로 있던 "요일로 고르기"는 "근무·요일" 안으로, "음력으로 고르기"는
     "기간" 안으로 흡수함.
     a. 근무·요일로 고르기: 근무×요일 표(세로: 근무 순서대로, 가로: 월~일)를
        보여주고 칸을 누르면 바로 켜지고(색칠) 다시 누르면 꺼짐 — 그게 다임.
        "조건 추가" 같은 별도 단계나 설명 문구 없음(색칠된 칸 자체가 곧
        반복 조건). 한 근무의 요일 7칸을 전부 켜면 "그 근무 날마다"(요일
        무관), 한 요일의 근무를 전부 켜면 "그 요일마다"(근무 무관)와 동일.
     b. 기간으로 고르기: 기본은 "3개월마다" 등 년/개월/일 간격(근무 주기와
        무관, 실제 달력 날짜로 계산. 전환 시작일 = 그 할 일의 원래 날짜, 새로
        만들 때는 오늘). 구석에 작은 "음력" 토글 버튼 — 누르면 화면이 음력
        월/일 입력으로 바뀌고 원래 날짜의 음력이 자동으로 채워짐(부모님
        생신 등 극소수 용도, "매년 음력 O월 O일마다"로 저장).
4) 실행: 오늘 탭에서 오늘 근무 + 오늘 할 것들을 한눈에 보고 체크.

## 반복의 3층 구조 (내부 엔진 개념)
- 층1 달력 반복: 요일·날짜 기준 (매주 화, 매월 15일)
- 층2 주기 반복: 근무 주기 기준 (D번호 엔진). 이 앱만의 차별점.
  ※ **사용자에게 D번호를 절대 직접 노출하지 않는다.** "비번날마다" 같은
    사용자 언어로 번역해 보여준다. D는 내부 계산용(코드 변수명 dayIndex로는
    계속 씀 — 화면에 글자로 찍히지만 않으면 됨). 한 번 날짜 상세의 "근무
    변경" 기능에서 "D3 · 주간"처럼 예외적으로 화면에 그대로 찍힌 적이
    있었음(발견 즉시 "이 자리마다"로 교체) — 새 화면·기능을 만들 때 이
    원칙을 항상 먼저 점검할 것.
- 층3 간격 반복: "3개월마다, 6주마다" 같은 실제 달력 날짜 간격 (머리, 세차 등).
  반복 방식 중 "기간으로 고르기"(rule.type:'interval')로 존재. 근무
  주기(cycleDays)와 무관하게 anchorDate + years/months/days로 계산.
  (예전에 따로 있던 "리마인더" 기능은 이걸로 대체돼서 제거함 — 다시 별도
  기능으로 만들지 말 것.)
- 음력 매년 반복: rule.type:'lunar', {lunarMonth, lunarDay}. "기간으로
  고르기" 화면 안의 하위 선택(promoteMode는 'interval' 그대로,
  promoteIntervalSubMode만 'lunar'). 매년 그 음력 월/일의 "평달" 발생에만
  맞춤(윤달 무시 — 매년 정확히 한 번씩만 걸리게). 그 달이 그 해에 30일이
  없으면 그 해는 자동으로 건너뜀. 한국천문연구원(KASI) 기준 음력 변환
  데이터(1000~2050년) — 날짜 상세 화면의 음력 표시와 같은 변환 엔진
  (solarToLunar/lunarToSolar) 공유.

## 보류/제외 (지금 안 만듦)
- 스킵(못 한 것 표시) 기능: 영구 제외.
- 회고·통계 대시보드·월간 리포트: 영구 제외(앱 정체성 밖).
- 웹(PWA) 버전 자체 알람: 브라우저 한계로 불가능(네이티브 앱 버전은 구현
  완료 — 아래 "근무유형별 알람" 참고).

## 근무유형별 알람 (네이티브 앱 전용, 구현 완료)
- 웹(브라우저/GitHub Pages) 버전에서는 안 울림 — 설치된 안드로이드 앱에서만
  동작(isNativeApp() 체크, 웹에서는 안내 문구만 보여주고 스킵).
- 설정 탭 "근무유형별 알람"(renderShiftAlarmSettings)에서 근무유형별 켜기/끄기 +
  시각 설정. state.shiftAlarms = { [근무이름]: {enabled, time:"HH:MM"} },
  저장은 바뀔 때마다 즉시(확인 버튼 없음). 시각 입력은 숫자 타이핑
  (`<input type="number">` 두 칸, .shift-alarm-time-input, change/blur 시
  0~23·0~59로 clamp) — **드래그 다이얼 방식이 아님**(다른 화면의 기간
  스피너 .promote-interval-field와는 다른 입력 방식, 섞어서 되돌리지 말 것).
  **목록에 나오는 근무 이름은 기본 순환 패턴뿐 아니라 dayOverrides(D번호
  예외)로 "근무 변경"해서 붙인 이름(예: 당직)도 포함**(2026-07-16 도입,
  기본 패턴에 없는 근무는 알람을 아예 켤 수 없었던 문제 수정) — 다시
  기본 패턴 이름만 모으는 방식으로 줄이지 말 것. **반대로 shiftOverrides
  (날짜 예외, 예: "괌여행"처럼 특정 날짜 하루만 바꾼 것)는 일부러 뺌**
  (2026-07-16 같은 날 재조정, 사용자 요청 — "일회성으로 바뀐 근무는 알람
  목록에 넣지 말아달라"). dayOverrides는 그 D번호(사이클 자리)가 돌아올
  때마다 반복 적용되는 변경이라 알람을 걸 대상이 되지만, shiftOverrides는
  그 날짜 단 하루만 적용되는 일회성 변경이라 알람 목록에 남을 이유가
  없음(어차피 그 날짜엔 getEffectiveShiftName이 그 일회성 이름을 반환해서
  원래 근무의 알람도 자동으로 안 울림 — 목록에서 빼는 건 UI만 정리하는
  것). renderShiftColorSettings()(근무유형 색 설정)는 이 구분과 무관하게
  계속 둘 다 보여줌 — 색상은 일회성 변경 근무에도 색이 필요하니 그대로
  유지, 알람 목록만 이렇게 분리됨.
- **알림 권한 꺼짐 경고 문구(2026-07-16 추가)**: scheduleShiftAlarms()는
  알림 권한이 없으면(ensureAlarmPermission()이 false) 예약 자체를 조용히
  건너뜀 — 그런데 화면엔 아무 표시가 없어서 "알람을 분명히 켰는데 시간이
  돼도 아무 반응이 없다"고 혼동한 실제 사용자 신고가 있었음(원인: 예전
  테스트 중 크래시가 반복되던 때 알림 권한 요청 창을 거부했거나 그렇게
  처리돼서, 안드로이드 13+ 정책상 한 번 거부되면 앱이 다시 물어봐도 창 자체가
  안 뜨고 조용히 거부로만 응답함 — 사용자가 휴대폰 설정에서 직접 켜야만
  풀림). 고침: renderShiftAlarmSettings()가 이 화면을 열 때마다 권한 상태를
  확인해서, 꺼져 있으면 맨 위에 빨간 안내 문구("알림 권한이 꺼져 있어서
  알람이 울리지 않아요. 휴대폰 설정 → 앱 → 루틴 → 알림에서 허용해주세요.")를
  보여줌(.shift-alarm-warning). 앱이 프로그램적으로 다시 권한 창을 띄울 수는
  없으므로(안드로이드 정책, 위 이유 참고) 문구로 안내하는 것이 최선 — 버튼을
  눌러 자동으로 설정 화면까지 이동시키는 것은 시도 안 함(가능하지만
  범위 밖으로 보류, 필요해지면 ensureFullScreenIntentAllowed()와 같은 방식의
  설정 화면 인텐트로 추가 가능).
- **기기 배터리 절약 기능이 알람을 막는 문제(2026-07-16, 실제 확인된 원인)**:
  위 알림 권한 경고를 추가한 뒤에도 한 사용자가 "스위치도 켜져 있고 시각도
  맞는데 알람이 전혀 안 울린다(빨간 경고 문구도 안 뜸 — 즉 알림 권한
  문제는 아니었음)"고 재신고 — 기종을 확인해보니 삼성 기기였고,
  **"설정 → 앱 → 루틴 → 배터리"를 "제한 없음"으로 바꾸니 바로 해결됨**.
  원인: setAlarmClock()이 안드로이드 표준 절전(Doze) 모드는 우회하지만,
  삼성(그 외 샤오미 등 중국계 제조사도 비슷한 자체 기능 있음)은 그와
  별개로 자체 배터리 관리 기능으로 백그라운드 앱을 더 강하게 제한할 수
  있어 이걸 코드로는 우회할 방법이 없음(기기 설정 영역). **고침**: 앱
  차원에서 더 손쓸 수 없으므로, renderShiftAlarmSettings() 안내 문구에
  "알람이 안 울리면 휴대폰 배터리 절약 기능이 이 앱을 막고 있을 수
  있어요. 설정 → 앱 → 루틴 → 배터리에서 '제한 없음'으로 바꿔보세요."를
  추가해서 미리 안내함(사용자가 직접 요청 — "설정탭에 안내 문구로 써놓기").
  기종별로 메뉴 경로가 조금씩 다를 수 있음(예: 삼성은 "배터리 및 디바이스
  케어 → 백그라운드 사용 제한"에 "잠자기 앱 목록"이 따로 있음) — 문구는
  가장 흔한 삼성 기준 경로 하나만 짧게 안내(전 기종 분기까지는 과함).
- **소리·진동 전체 켜기/끄기(2026-07-16 추가)**: 근무별이 아니라 알람
  기능 전체에 적용되는 토글 두 개(state.shiftAlarmSoundEnabled/
  shiftAlarmVibrateEnabled, 기본값 둘 다 true) — renderShiftAlarmSettings()
  맨 위, 근무별 목록보다 먼저 보임(.shift-alarm-global-row, 근무별 줄보다
  패딩·글자 크기를 줄인 전용 클래스). **실제 스위치 모양의 토글
  (.toggle-switch)로 표시**(2026-07-16 재조정 — 처음엔 "켜짐"/"꺼짐" 글자가
  적힌 알약 모양 버튼(.shift-alarm-toggle)이었는데, 사용자가 "토글 형태로
  넣어달랬는데 버튼식으로 나왔다"고 지적해서 실제 스위치 모양(동그란 손잡이가
  좌우로 움직이는 형태)으로 바꿈 — 글자 없이 켜짐/꺼짐을 위치와 색으로만
  표현. 근무별 "이 근무 알람 켜기" 버튼(.shift-alarm-toggle, "켜짐"/"꺼짐"
  글자 버튼)은 이 요청 대상이 아니어서 그대로 둠 — 소리·진동 두 개만
  스위치 모양으로 바꾼 것, 혼동해서 근무별 버튼까지 같이 바꾸지 말 것).
  바뀔 때마다 scheduleShiftAlarms()를
  다시 불러서 네이티브(ShiftAlarmPlugin)에 최신 값을 넘김 — 네이티브는
  SharedPreferences("widget_bridge")의 KEY_SOUND_ENABLED/KEY_VIBRATE_ENABLED에
  저장해두고, **예약 시점이 아니라 알람이 실제로 울리는 시점**(ShiftAlarmActivity.
  startRinging())에 그 값을 읽어서 반영함 — 켰다 껐다 한 뒤 아직 안 울린
  알람도 항상 최신 설정을 따름. 소리를 꺼두면 MediaPlayer 자체를 안 만들고,
  진동을 꺼두면 Vibrator.vibrate() 자체를 안 부름.
  **"기기의 무음/진동 모드와 무관하게 이 앱 설정이 우선"(요청 그대로)**: 소리는
  AudioAttributes.USAGE_ALARM 스트림으로 재생하는데, 안드로이드가 이 스트림을
  무음/진동 모드·방해금지 모드와 별개로 취급해서 그대로 남 — 별도 코드
  없이 이미 되던 부분. 진동도 알림 채널을 거치지 않고 Vibrator.vibrate()를
  직접 부르는 방식이라 마찬가지로 기기 무음/진동 모드와 무관하게 동작함
  (채널을 거치는 진동만 무음/진동 모드의 영향을 받음 — 이 앱은 채널
  소리·진동을 아예 꺼뒀으므로 해당 없음, 아래 항목 참고). 기기 자체의
  "알람" 볼륨이 0이거나 진동을 시스템에서 완전히 꺼둔 극히 드문 경우만
  예외(앱이 어떻게 할 수 없는 부분).
- **"진짜 기상 알람"으로 재구현(2026-07-16)** — 처음엔 짧게 떴다 사라지는
  일반 알림이었는데, 사용자가 "화면 전체를 덮고, 사용자가 꺼야 꺼지는
  알람"을 요청해서 완전히 새로 만듦. @capacitor/local-notifications
  플러그인은 이런 동작(화면 강제로 띄우기, 끄기 전까지 소리 반복)을 지원
  안 해서, 이 기능만 안드로이드 코드로 직접 만든 전용 부품 3개로 구현함
  (WidgetBridgePlugin처럼 이 프로젝트 전용으로 새로 쓴 것 — npm 패키지
  아님):
  - **ShiftAlarmScheduler**(그냥 자바 클래스, 플러그인 아님) — 알람 하나를
    실제로 예약/취소하는 공통 로직. `AlarmManager.setAlarmClock()`을 씀 —
    "진짜 알람시계" 앱을 위한 API라 안드로이드 12+에서 정확한 시각 알람에
    보통 필요한 별도 권한(SCHEDULE_EXACT_ALARM) 없이도 항상 정확한 시각에
    울리고 절전(Doze) 상태에서도 깨움. 예전 방식(LocalNotifications +
    allowWhileIdle)보다 이 앱이 원하는 동작에 더 정확히 맞아서 이걸로
    바꿈 — 다시 LocalNotifications 기반 예약으로 되돌리지 말 것.
  - **ShiftAlarmReceiver** — 예약된 시각에 안드로이드가 깨우는
    BroadcastReceiver. 여기서 화면을 직접 띄우지 않음(백그라운드 상태에서
    액티비티를 바로 띄우는 건 안드로이드 10+부터 막혀 있어서) — 대신
    `setFullScreenIntent()`가 달린 알림을 하나 올려서 안드로이드가 알아서
    화면을 띄우게 함(전화 수신 화면과 같은 원리). 화면이 꺼져있거나
    잠겨있으면 안드로이드가 자동으로 전체 화면으로 띄우고, 화면이 켜져
    있고 잠금이 풀려있으면 대신 위에서 살짝 뜨는 알림으로만 보여주고
    눌러야 열림 — 둘 다 안드로이드가 정한 동작이라 앱에서 더 강제할 수
    없음.
  - **ShiftAlarmActivity** — 알람이 울릴 때 뜨는 화면 자체. `setShowWhenLocked`/
    `setTurnScreenOn`(API 27+, 그 미만은 옛 윈도우 플래그)으로 잠금화면
    위로 뜨게 함. 소리(RingtoneManager의 시스템 "기본 알람음",
    AudioAttributes.USAGE_ALARM, MediaPlayer로 반복 재생)와 진동
    (VibrationEffect.createWaveform 반복)이 "끄기"를 누르기 전까지 계속
    남 — **뒤로가기는 막아둠(onBackPressed에서 아무 것도 안 함)**, 오직
    "끄기"/"5분 뒤 다시"(스누즈, ShiftAlarmScheduler.scheduleOne을 5분
    뒤 시각으로 다시 호출) 버튼 두 개로만 멈춤. 다시 뒤로가기로 닫히게
    하거나, 일정 시간 뒤 자동으로 꺼지게 만들지 말 것 — "사용자가 꺼야
    꺼지는 알람"이 이 기능의 핵심 요구사항.
  - **ShiftAlarmPlugin**(Capacitor 플러그인) — JS가 "이 날짜들에 이 근무
    이름으로 알람을 걸어야 한다"(scheduleShiftAlarms()가 계산)를 통째로
    넘기면(scheduleAll({items, sound, vibrate})) 기존 예약을 전부 취소하고
    새로 깖(예전 LocalNotifications 방식과 같은 "통째로 다시 깔기" 원칙
    유지, id는 여전히 YYYYMMDD 날짜 숫자) — sound/vibrate 두 값(아래 "소리·
    진동 전체 켜기/끄기" 항목 참고)도 이때 같이 SharedPreferences에 저장됨.
    ensureFullScreenIntentAllowed()도 이 플러그인에 있음 — 안드로이드
    14(API 34)부터는 "화면 강제로 띄우기" 권한이 자동으로 안 켜져 있어서,
    이 메서드가 확인해보고 안 켜져 있으면 그 설정 화면으로 바로 안내함.
    **호출 위치 버그(수정 완료, 2026-07-16)**: 처음엔 scheduleShiftAlarms()
    안에서 곧바로 불렀는데, 그 함수는 **앱을 켤 때마다(초기화 시점) 항상
    자동으로도 실행되는 함수**라서 — 이미 알람이 켜져 있는 상태로 앱을
    다시 열면 화면이 뜨기도 전에 곧바로 설정 화면으로 튕겨나가버려서
    "앱이 안 열린다"는 것처럼 보이는 버그가 있었음(실제 사용자 신고 —
    앱은 정상이고 설정 화면이 그 위로 뜬 것뿐이었음). JS의
    ensureFullScreenIntentIfNeeded()로 분리해서 **renderShiftAlarmSettings()
    (사용자가 실제로 "근무유형별 알람" 설정 화면을 보고 있을 때)에서만
    부르도록 고침** — 세션당 한 번(_fullScreenIntentChecked)은 그대로 유지.
    **다시 앱 초기화 경로(scheduleShiftAlarms() 등 앱을 열 때마다 자동으로
    도는 함수)에서 이 확인을 부르지 말 것** — 항상 사용자가 그 설정과
    관련된 화면을 보고 있을 때만 부를 것.
    **진짜 원인은 따로 있었음(2026-07-16, 같은 날 재조사)**: 위 수정을 배포한
    뒤에도 사용자가 "여전히 안 열린다"고 재신고 — 이번엔 "잠깐 떴다가 바로
    꺼짐(오류 문구 발생)"이라는 더 정확한 증상을 알려줘서, 화면 전환이 아니라
    **진짜 충돌(크래시)**임을 확인함. scheduleShiftAlarms()는 앱을 켤 때마다
    항상 자동으로 실행되는데, 그 안에서 부르는 ShiftAlarmPlugin.scheduleAll()의
    반복문이 JSON 형식 오류만 잡고 있었을 뿐, 안드로이드가 알람을 실제로 거는
    도중 생길 수 있는 다른 문제(기기별 특이 동작 등)는 하나도 안 잡고 있었음
    — 그래서 알람 하나를 거는 데서 뭔가 문제가 생기면 그대로 앱 전체가
    죽어버리는 구조였음. **고침**: ① scheduleAll()의 반복문을 "항목 하나당
    하나씩" 방어적으로 처리(한 알람이 실패해도 나머지는 계속 진행, 앱은 안
    죽음)로 바꿈 ② 알람이 실제로 울리는 코드(ShiftAlarmReceiver), 알람 화면
    (ShiftAlarmActivity.onCreate), 앱 시작 코드(MainActivity.onCreate의 알람
    관련 두 줄)까지 전부 "이 부분에서 무슨 문제가 생겨도 이 기능 하나만 조용히
    안 되고 앱 전체는 반드시 열려야 한다"는 원칙으로 방어적으로 감싸둠. **앞으로
    알람 기능에 코드를 추가할 때도 항상 이 원칙을 지킬 것** — 특히 앱을 켤
    때마다 자동으로 도는 코드(scheduleShiftAlarms() 계열)는 그 안에서 뭐가
    잘못돼도 절대 앱 전체를 못 열게 만들면 안 되므로 더 엄격하게 방어적으로
    짤 것.
  - 알림 채널("shift-alarm")은 여전히 MainActivity.ensureAlarmChannel()에서
    네이티브로 직접 만듦 — 다만 **이제 소리·진동이 전혀 없는 조용한
    채널**(channel.setSound(null,null), enableVibration(false))로 바뀜.
    소리·진동은 전부 ShiftAlarmActivity가 반복 재생을 직접 책임지므로,
    채널에도 소리를 주면 화면이 뜨기 전 아주 잠깐 겹쳐 울리는 어색함이
    생길 수 있어서 뺌 — importance만 HIGH로 유지(fullScreenIntent가
    동작하려면 필요). 다시 이 채널에 소리를 넣지 말 것.
  - JS의 ensureAlarmPermission()(알림을 띄워도 되는지 허락 확인)은 그대로
    @capacitor/local-notifications 플러그인의 권한 확인창을 재사용함 —
    화면 자체는 새로 만들었지만 "알림 허락받기"는 이미 잘 되던 부분이라
    새로 안 만듦.
- **예전에 겪었던 문제들(지금 구조에서는 해당 없음, 기록만 남김)**: 채널
  importance가 유효 범위(0~4) 밖인 5였던 것, createChannel의 vibrate 기본값이
  false였던 것, allowWhileIdle이 없어서 Doze 중 예약이 약했던 것 — 전부
  LocalNotifications 플러그인을 쓰던 시절의 문제였고, setAlarmClock() 기반
  구조로 바뀌면서 근본적으로 해당 사항이 없어짐.
- **남은 한계**: 기기 자체의 "알람" 볼륨이 0이면(무음/진동 모드와는 별개의,
  시스템 설정 안의 독립된 볼륨) 진짜 알람시계 앱도 마찬가지로 소리가 안
  남 — 사용자가 직접 그 볼륨을 올려야 함. 화면이 켜져 있고 잠금이 풀려있는
  상태에서 알람이 울리면 전체 화면 대신 일반 알림으로만 뜰 수 있음(안드로이드가
  정한 동작, 뒤 "ShiftAlarmReceiver" 항목 참고).

## 배포
- GitHub Pages. 저장소 routine-app.
- 배포 = git add → commit → push.
- 접속: https://gmdwn916-cmd.github.io/routine-app/
- 데이터는 기기 localStorage에만. 코드 배포는 사용자 데이터에 영향 없음.
- **"빌드해줘" = 안드로이드 앱 빌드 + 배포(git add/commit/push)를 한 번에,
  끝까지 처리**(2026-07-16 확정) — 사용자가 "빌드"라고 말하면 그때 모아뒀던
  변경사항을 안드로이드 APK로 빌드해서 전달하고, 같은 시점에 GitHub Pages
  배포까지 같이 끝내는 것으로 통일함. 둘 중 하나만 하고 멈추지 말 것.

## 파일 구조
- index.html 단일 파일(HTML/CSS/JS 전부)에 앱 로직 전체가 있음 — android/www는
  이 파일을 감싸는 껍데기일 뿐 로직 없음. 번들러 안 씀. Capacitor 다리
  스크립트 2개(capacitor.js, local-notifications.js — node_modules에서 그대로
  복사한 순수 연결 코드, 로직 없음)만 예외로 로드됨 — **이 두 파일은
  index.html과 같은 위치(루트)에도, www/ 안에도 똑같이 있어야 함**(루트는
  GitHub Pages 배포용, www는 안드로이드 앱용). 새 Capacitor 플러그인을
  추가하면 그 플러그인의 dist/plugin.js도 같은 방식으로 루트+www에 복사하고
  index.html에 스크립트 태그를 추가해야 함(빌드 도구가 없어서 수동으로 하는
  것). 웹에서는 이 스크립트들이 로드는 되지만 Capacitor.isNativePlatform()이
  false라서 실제로는 아무 것도 안 함.
- Capacitor 안드로이드 네이티브 앱 껍데기(위젯·자체 알람 등 PWA로는 안 되는
  기능용):
  - package.json/node_modules — Capacitor 패키지 설치용(node_modules는
    .gitignore 대상).
  - capacitor.config.json — 앱 이름 "루틴", 패키지ID `com.hyeongju.routineapp`,
    webDir: `www`.
  - **www/index.html은 루트 index.html의 복사본, 직접 고치면 안 됨** — 항상
    루트를 고친 뒤 `cp index.html www/index.html && npx cap sync android`를
    **같이** 실행해야 android/app/src/main/assets/public/index.html(APK에
    실제로 들어가는 파일)에 반영됨. `cp`만 하고 `npx cap sync`를 빼먹으면
    Gradle이 계속 옛 코드로 빌드함 — 증상이 "웹뷰 캐시 문제"처럼 보이기
    쉬우므로, "고쳤는데 안 바뀐다" 싶으면 캐시부터 의심하지 말고
    `diff www/index.html android/app/src/main/assets/public/index.html`
    (또는 APK 안 실제 내용을 unzip해서 확인)로 반영 여부부터 볼 것.
  - android/ — Capacitor가 생성한 네이티브 프로젝트(Android Studio로 엶).
    gradle.properties의 `android.overridePathCheck=true`는 폴더명(루틴어플)에
    한글이 섞여 나오는 경고를 끈 것(네이티브 C++ 코드가 없어서 안전) — 지우면
    빌드 실패.
  - 빌드: `cd android && ./gradlew assembleDebug`(JAVA_HOME을 Android Studio
    내장 JDK로 지정, 보통 `C:\Program Files\Android\Android Studio\jbr`).
    결과물: android/app/build/outputs/apk/debug/app-debug.apk.
- **데이터 백업(내보내기/가져오기, 설정 탭)**: 웹과 네이티브 앱은 localStorage가
  서로 다른 저장공간이라 자동으로 안 이어짐 — JSON으로 내보내고 다른 실행
  환경에서 가져오기 하면 이어붙일 수 있음(가져오기는 확인창 후 localStorage
  통째로 덮어쓰고 location.reload(), 마이그레이션은 그 뒤 초기화 과정에서
  평소처럼 자동으로 돎). **내보내기 방식이 웹/네이티브에서 다름**
  (isNativeApp()으로 분기): 웹은 blob 다운로드, 네이티브 웹뷰는 blob
  다운로드를 자체 처리 못 해서(플랫폼 자체 한계) WidgetBridgePlugin.
  saveBackupFile()로 안드로이드가 다운로드 폴더에 직접 씀(MediaStore API,
  안드로이드 10+; 9 이하는 WRITE_EXTERNAL_STORAGE 권한 필요) — 이 구분을
  하나로 합치려 하지 말 것.
- MainActivity는 웹뷰 캐시를 안 씀(LOAD_NO_CACHE, 방어적 조치) — 다만
  "고쳤는데 안 바뀐다"의 진짜 원인은 대개 캐시가 아니라 위 www/index.html
  동기화 실수였음, 캐시부터 의심하지 말 것.

## 위젯 공통 규칙 (다섯 개 위젯 모두 적용 — 새 위젯 만들 때 먼저 참고)
- **네이티브에는 근무 계산 로직을 두지 않는다**: 근무·할일 계산은 항상
  JS(index.html)가 끝내서 결과만 넘김(WidgetBridgePlugin의 setXXXData 계열
  메서드) — 계산 규칙이 두 곳에 있으면 나중에 한쪽만 고쳐서 어긋날 위험.
  네이티브가 직접 계산해도 되는 유일한 예외는 "오늘이 며칠인지" 같은 근무
  로직과 무관한 단순 사실(각 위젯 항목의 fallback 코드 참고, 예: 오늘 위젯
  데이터가 아직 없을 때 SimpleDateFormat으로 오늘 날짜만 대신 계산).
- **PendingIntent 충돌 방지**: 서로 다른 목적지로 가야 하는 PendingIntent는
  반드시 requestCode 또는 action 중 하나 이상을 다르게 줄 것(이 프로젝트는
  둘 다 주는 걸 습관으로 함). PendingIntent의 정체성은 (컴포넌트,
  requestCode, action/data/category)로 결정되고 **extras는 정체성에 안
  들어감** — 같은 조합이면 나중 것이 앞의 것을 덮어써서, 여러 위젯이 "같은
  버튼"으로 취급되고 마지막에 갱신된 위젯의 목적지로만 열리는 사고가 남(실제
  발생: 위젯을 눌러도 전부 미배치 화면으로만 열리던 버그, 원인은 네 위젯의
  "앱 열기" 인텐트가 전부 같은 컴포넌트(MainActivity)+같은
  requestCode(0)였던 것 — 위젯마다 고유한 action 문자열을 붙여서 해결).
- **라이트/다크 배경-글자색 동기화**: 위젯 배경을 XML의 `@drawable` 참조로
  그냥 두면 런처가 매번 "그 순간 자신의" 테마로 실시간 재해석하는데,
  글자색(`setTextColor`)은 우리가 push한 순간에 이미 확정돼 심어진 값이라
  나중에 시스템 테마만 바뀌면 서로 어긋남(흰 배경에 흰 글자로 안 보이는
  사고). 배경도 매번 직접 판단해서 `widget_background_light.xml`/
  `widget_background_dark.xml`(색이 고정된 채로 따로 있는 드로어블, `@color`
  참조 없음) 중 하나를 `setBackgroundResource`로 명시적으로 심을 것 —
  다섯 위젯 전부 이 방식. **다크 여부 판단은 이제 순수 시스템 설정이
  아니라 `WidgetThemeHelper.isDarkMode()`를 씀(2026-07-18 추가)** —
  앱 안 설정(설정 탭 "테마")에서 라이트/다크를 명시적으로 고르면 휴대폰
  시스템 설정과 달라도 그 값을 최우선으로 따르고, "시스템"을 고른 경우만
  기존처럼 `Configuration.uiMode`를 봄(SharedPreferences("widget_bridge")의
  `widget_theme_override` 키, JS의 applyTheme()이 테마를 바꿀 때마다
  WidgetBridgePlugin.setThemeOverride()로 갱신). 원인이었던 버그: 앱 안에서
  "다크"로 바꿔도 위젯은 항상 시스템 설정만 보고 있어서, 휴대폰 자체는
  라이트인 채로 앱만 다크로 바꾸면 위젯이 안 따라오는 것처럼 보였음. 글자색
  중 라이트/다크에 따라 실제로 값이 다른 건 배경과 `widget_text_primary`
  뿐이라(`widget_text_secondary`는 라이트/다크 값이 동일) `WidgetThemeHelper.
  primaryTextColor()`도 같이 씀 — 새 위젯을 만들 때 이 두 함수를 그대로
  재사용할 것, 직접 `Configuration.uiMode`를 다시 읽지 말 것.
  **앱을 안 연 채로 휴대폰 시스템 설정만 바뀌는 경우(2026-07-18 추가)**:
  위 판단은 push 시점(앱 데이터 변경, 테마 변경, 앱 재개(visibilitychange))
  에만 다시 이뤄지므로, 앱을 아예 안 열고 위젯만 홈 화면에 떠 있는 채로
  시스템 다크/라이트가 바뀌면(주로 "시스템" 테마를 쓸 때) 그 순간엔 위젯이
  갱신될 계기가 없어서 예전 밝기 그대로 남는 문제가 있었음(리스트 위젯
  (오늘·미배치)에서 특히 눈에 띔 — 사용자 신고로 발견). 다섯 위젯의
  매니페스트 `<intent-filter>`에 `android.intent.action.CONFIGURATION_
  CHANGED`를 추가하고 각 Provider의 onReceive()에서 이 액션을 받으면
  refreshAll()을 부르도록 고침 — **다만 이 브로드캐스트가 매니페스트로
  등록한 리시버에 최신 안드로이드에서도 항상 확실히 오는지는 실기기로
  확인 못 함**(안드로이드 8+의 암시적 브로드캐스트 제한 대상인지 문서로
  100% 확실친 않음) — 안 와도 부작용은 없고(예전과 같은 상태일 뿐),
  최소한 앱을 열면(visibilitychange) 확실히 갱신되는 기존 경로는 그대로
  살아있음. 실기기에서 안 되는 게 확인되면 그때 다른 방식(예: 더 짧은
  주기의 updatePeriodMillis 폴링 등, 배터리 비용 있음)을 검토할 것.
- **RemoteViews가 못 그리는 것**: 순수 `<View>`(스페이서 등)를 쓰면 위젯이
  "추가할 수 없음" 오류로 죽음 — 빈 자리가 필요하면 내용 없는 `<TextView>`를
  쓸 것. `setBackgroundColor`는 항상 각진 사각형으로 칠해서 모서리를 둥글게
  할 수 없음 — 둥근 배지가 필요하면 둥근 밑그림 ImageView를 밑에 깔고
  `setColorFilter`로 색만 입힐 것(그 밑그림을 담는 부모는 `RelativeLayout` +
  형제 뷰 기준 정렬(`layout_alignLeft/Top/Right/Bottom`)로 크기를 잡을 것 —
  `FrameLayout(wrap_content)` 안에 `match_parent` 자식만 두면 크기가 0으로
  무너지는 버그가 있어서 쓰지 말 것).
- **중첩 클릭 영역**: 보통 RemoteViews 레이아웃(스케줄 위젯의 날짜 칸처럼)에서는
  자식 뷰에 자기만의 PendingIntent를 걸면 표준 안드로이드 터치 처리대로
  자식이 있는 자리는 자식이, 없는 자리는 부모가 받음 — 별도 처리 불필요.
  반면 **컬렉션 위젯**(RemoteViewsService+RemoteViewsFactory로 만든
  ListView, 오늘 할일·미배치 위젯이 이 방식)에서는 자식이 부모의
  fillInIntent를 자동으로 물려받지 않음 — 반응하길 원하는 뷰마다 각각
  fillInIntent를 걸어야 함.
- **위젯에서 뜨는 작은 입력창(다이얼로그)의 다크 모드 글씨색**: QuickAddActivity/
  TodayQuickAddActivity/DayQuickViewActivity 셋 다 QuickAddDialogTheme(부모가
  `Theme.AppCompat.Light.Dialog`로 고정, DayNight 아님)을 씀 — 카드 배경
  (quick_add_bg.xml)은 `@color/widget_bg`를 참조해서 다크 모드에 맞춰 어두워
  지지만, 그 안의 EditText는 테마가 라이트로 고정돼 있어서 글자/힌트 색은
  계속 밝은 배경 기준(까만 글씨) 그대로 나옴 — 다크 모드에서 어두운 카드 위에
  까만 힌트 글씨가 남아 안 보이는 버그가 있었음(수정 완료, 2026-07-16). 이런
  입력창을 새로 만들 때는 EditText에 `android:textColor`/`textColorHint`를
  `@color/widget_text_primary`/`widget_text_secondary`로 항상 명시적으로 줄
  것 — 테마가 알아서 맞춰줄 거라고 가정하지 말 것.
- **위젯을 탭하면 관련 화면으로 이동**: "앱 열기" 인텐트에 목적지
  (widget_nav="month"|"today"|"inbox", 달력/스케줄 위젯은
  widget_nav_month="YYYY-MM"도 같이)를 실어 보내고, MainActivity.onCreate/
  onNewIntent(launchMode=singleTask라 앱이 이미 떠 있으면 onNewIntent로
  옴 — 둘 다 처리)가 이 값을 바로 JS에 넘기지 않고 SharedPreferences에
  저장만 해둠(웹뷰가 아직 로딩 중일 수 있어 타이밍이 불안정하기 때문) → JS의
  syncWidgetNavTarget()이 앱 시작·포그라운드 복귀 시 읽어서 이동
  (switchTab('month'/'today') 또는 openInbox()) 후 비움. 새 위젯을 추가할
  때도 이 패턴(네이티브는 저장만, JS가 다음 동기화 시점에 가져감)을 유지할
  것 — 인텐트 시점에 바로 JS 실행을 시도하지 말 것. 달력/스케줄 위젯이
  "그 달"을 알아내는 방법은 이미 JS가 계산해 저장해둔 데이터에서 지금
  보여주는 날짜 하나를 잘라 쓰는 것뿐(currentDisplayedMonth(), 근무 계산
  아님).
- **임시 우편함(pending) 패턴**: 위젯에서 사용자가 뭔가 입력/변경하면
  SharedPreferences("widget_bridge")에 임시로 쌓아두고, JS가 앱 시작·
  포그라운드 복귀 시 WidgetBridgePlugin으로 읽어서 진짜 데이터에 반영한 뒤
  비움 — 미배치 추가("pending_inbox_items", 문자열 배열), 날짜 지정
  추가("pending_dated_items", {text,date} 배열), 오늘 위젯 체크
  ("today_widget_pending_toggles", 토글이 아니라 최종 상태를 담음 — 중복
  반영돼도 결과가 안 달라지게), 위젯 이동 목적지("pending_nav_target") 전부
  이 패턴. 새 위젯에 "쓰기"가 필요하면 이 패턴을 그대로 따를 것.
- **쉼표(,)로 연속 입력(2026-07-18 추가)**: QuickAddActivity/
  TodayQuickAddActivity/DayQuickViewActivity 셋 다(글자 입력이 있는 위젯
  화면 전부) EditText에 TextWatcher를 달아서, 쉼표를 치는 순간 그때까지
  쓴 글자를 바로 pending 저장소에 넣고 입력칸은 비운 채로 계속 입력할 수
  있게 함(앱 화면의 quick-add-input과 같은 목적, 동일한 날 같이 구현 —
  위 "달력·위젯 통합 관리" 원칙과 별개로 이건 애초에 항상 웹+위젯 같이
  가야 하는 기능이라 처음부터 셋 다 적용함). afterTextChanged 안에서
  EditText.setText()를 다시 부르면 리스너가 또 불려서 무한 루프가 되므로,
  그 순간만 리스너를 뗐다 다시 붙이는 방식으로 재귀를 막음 — 엔터/완료
  (IME_ACTION_DONE)로 마지막 조각을 저장하고 닫는 기존 흐름은 그대로 둠
  (쉼표 뒤에 남은 마지막 글자만 그 경로를 탐).
- **갱신 시점**: 근무·할일·설정에 영향 주는 모든 저장 시점(교대 주기, 주
  시작 요일, 근무 편집(색·이름·추가/삭제), 날짜별/D번호 근무 수정, 카테고리,
  할 일 추가/수정/삭제 등)에서 pushAllWidgets()를 호출 — 위젯이 늘어나도 이 함수
  안에만 새 push 호출을 추가하면 됨, 각 저장 시점 코드는 안 건드려도 됨.
  **한계(공통)**: 이 시점들 외에는 안 밀어줌 — 예를 들어 날짜가 자정을
  넘어가는 순간 앱을 며칠간 안 열면 위젯이 지난 데이터를 계속 보여줄 수
  있음. 앱을 한 번이라도 열면 바로 갱신됨.

## 위젯 1 — 빠른 할일 추가 (QuickAddWidgetProvider / QuickAddActivity)
- 위젯을 탭하면 작은 입력창(다이얼로그 테마 — 화면 전체 안 가리고 살짝
  뜸)이 뜨고, 글자를 넣고 엔터 치면 앱을 열지 않고 그 자리에서 저장됨.
  **확인 버튼 없음**(키보드의 완료 키로 저장 — 다시 버튼을 넣지 말 것).
- 입력한 글자는 SharedPreferences("widget_bridge")의 "pending_inbox_items"에
  쌓이고, WidgetBridgePlugin.getPendingItems/clearPendingItems를 거쳐 JS의
  syncWidgetInboxItems()(앱 시작 + 포그라운드 복귀 시 호출)가 state.inbox로
  옮김. 위젯은 "글자 목록"만 넘길 뿐 inbox 데이터 형식(id/createdAt 등)을
  전혀 모름 — 실제 inbox 항목으로 바꾸는 건 JS쪽에서만 함.
- QuickAddActivity는 `taskAffinity=""` + 위젯 쪽 Intent의
  `FLAG_ACTIVITY_NEW_TASK|MULTIPLE_TASK` 조합으로 MainActivity와 완전히
  다른 작업(task)으로 뜸 — 안 하면 앱이 이미 켜져 있을 때 입력창과 함께 앱
  화면까지 같이 튀어나옴. `windowSoftInputMode="stateAlwaysVisible"` +
  코드에서 명시적 showSoftInput 호출로 입력창 뜨자마자 키보드도 자동으로 뜸.
- **크기 1x1(40dp) 고정, 리사이즈 불가** — 사용자가 명시적으로 이 크기
  유지를 원함, 키우지 말 것. 레이아웃(widget_quick_add.xml): 글씨 칸
  (weight 15, textSize 8sp 고정 — 40dp 폭에서 "할 일 추가"가 안 잘리는
  걸로 확인된 값, 세로 비율을 바꿔도 이 글씨 크기는 안 바꿀 것) + 십자가
  칸(weight 45, ImageView는 `layout_width/height="match_parent"` +
  `scaleType="centerInside"` 필수 — 없으면 칸이 정사각형이 아닐 때 그림이
  옆으로 늘어나거나 눌림). 단순한 2칸 구조로 정착함 — 여백을 여러 칸으로
  잘게 쪼개는 방향(위여백/글자/간격/십자가/아래여백 등)으로 돌아가지 말 것
  (절대 위치·잘게 쪼갠 비율 둘 다 시도했다가 실기기에서 안 예뻐 보이거나
  안 맞아서 실패한 경험 있음). ic_widget_add.xml: 24x24 캔버스 중 20x20을
  채우는 파란색(#007AFF) 십자가(pathData로 직접 그림).

## 위젯 2 — 이번 달 달력 (MonthCalendarWidgetProvider, 읽기 전용)
- 4x4 기본, 리사이즈 가능. 날짜별 근무색을 큰 글씨로 보여줌(할 일은 안
  보여줌) — 목적이 "그날 근무가 뭔지 한눈에 파악"이라 여백은 유지하고
  글씨만 키움(날짜 숫자 16sp, 근무 이름 13.6sp — 각각 20% 축소, 요일 헤더는
  그대로 17sp). 모든 날짜 칸(42칸)의 세로 정렬은 `top|center_horizontal`
  (칸 위쪽에 붙음) — 오늘 칸도 배경·테두리만 다를 뿐 정렬 방식은 동일.
- 근무 계산은 전부 JS가 함: buildOneMonthGrid(year, month, weekStart)가
  renderGrid()와 완전히 같은 규칙(getEffectiveShiftName/getShiftColor
  재사용)으로 한 달치 42칸 그리드를 만들고, buildMonthCalendarPayload()가
  **[지난달, 이번달, 다음달] 3개월치를 항상 이 순서·개수로** 계산해
  pushMonthCalendarToWidget()이 넘김. payload: { headers(7),
  satCol/sunCol, months: [{monthLabel, days(42, null 또는
  {date,dayNum,shiftName,color,batchId,holidayName})}]×3 } —
  batchId/holidayName은 2026-07-18 추가(아래 두 항목 참고).
- "오늘" 강조(파란 글자 + widget_today_cell_border 테두리)와 토(#007AFF)/
  일(#FF3B30) 헤더 색칠만 네이티브가 직접 처리(JS가 알려준 날짜 문자열/열
  번호를 쓰는 것뿐이라 근무 로직 중복 아님).
- **일괄수정만 이어붙이기(2026-07-18 추가)**: 같은 근무가 옆 칸까지
  이어지면 배경은 계속 이어붙이되 글자는 그 구간 첫 칸에만 씀(예:
  "괌여행") — 단, **판단 기준은 이름이 같은지가 아니라 같은 batchId를
  가진 날짜 예외인지**(앱 화면 appendShiftIndicator와 동일 기준). 처음엔
  "이름이 같으면"으로 판단해서 하루씩 따로 바꾼 날짜나 기본 패턴의 우연한
  반복까지 잘못 합쳐 보이는 버그가 있었음 — batchId 비교로 교체. **줄(주)이
  바뀔 때 강제로 초기화하지 말 것**(2026-07-18, 같은 날 재수정) — days
  배열은 항상 날짜순이라 배열의 바로 앞 칸이 곧 "어제"이므로, 토→일로
  넘어가며 이어지는 일괄수정도 배열 인덱스만 보면 그대로 이어짐. 줄마다
  초기화했더니 그 경계에서만 이름이 다시 튀어나오는 버그가 있었음(앱
  화면은 "줄" 개념이 없이 실제 어제 날짜만 봐서 이 버그가 없었음) — 그
  달에 안 속하는 빈 칸(null)을 만났을 때만 진짜로 끊긴 것이므로 그때만
  초기화.
- **공휴일 이름 표시(2026-07-18 추가)**: 날짜 칸 전용 표시 자리가 따로
  없어서, 날짜 숫자 TextView 안에 SpannableStringBuilder로 작게(0.62배)
  이어붙임(RelativeSizeSpan + ForegroundColorSpan, 레이아웃 XML은 안
  건드림) — 색은 웹의 --holiday-color(#d9645e, 채도 낮춘 톤)와 같은 값을
  네이티브에도 그대로 상수로 둠(공유 리소스가 아니라 각 위젯 파일에 하드
  코딩돼 있음 — 값을 바꿀 땐 두 위젯 파일(달력·스케줄) + 웹 CSS 변수
  세 군데를 같이 맞출 것).
  **버그(수정 완료, 2026-07-16)**: 오늘 표시 테두리가 날짜가 지나도 안
  사라지던 문제 — RemoteViews는 매번 지정한 속성만 다시 적용하고 지정 안
  한 속성은 이전 상태 그대로 유지하는데, cell_container_N의 테두리
  배경(setBackgroundResource)이 "오늘"인 칸에서만 매번 새로 지정되고
  전체 초기화 루프에는 리셋이 빠져 있었음 — 그래서 어제까지 오늘이었던
  칸이 테두리를 계속 들고 있었음. 위젯 3(스케줄)의 sch_cell_N은 처음부터
  리셋이 있었는데 이 위젯만 빠져 있던 것 — 42칸 초기화 루프에
  `cell_container_N`의 배경을 매번 투명(setBackgroundColor 0)으로 리셋하는
  줄을 추가해서 고침. 앞으로 칸마다 조건부로 배경/테두리를 입히는 새 위젯을
  만들 때는 항상 전체 초기화 루프에 그 배경의 리셋도 같이 넣을 것.
- **이전/다음 달 넘기기**: 화살표 버튼 없음 — 월요일 쪽 세로줄(헤더+6줄
  그리드 전체) 전체를 누르면 이전, 일요일 쪽 세로줄 전체를 누르면 다음
  (자기 자신에게 보내는 ACTION_PREV/NEXT 브로드캐스트로 표시 인덱스만
  바꿔 다시 그림, 앱은 안 열림). 어느 열이 월/일요일인지는
  `mondayCol = (sunCol+1)%7`로 계산(JS가 넘겨준 sunCol 기준, weekStart와
  무관하게 성립하는 항등식). 딱 3개월치만 있어서 그 범위 밖은 앱을 열어야
  갱신됨.

## 위젯 3 — N주 스케줄 (ScheduleWidgetProvider)
- 요일 헤더 + 2주(2줄×7칸) 그리드(**목록형이 아님 — 다시 세로 목록으로
  돌아가지 말 것**, 참고 앱 캡처를 보고 그리드로 재설계한 것이 최종 결정).
  칸 = 날짜 숫자 + 근무 배지 + 할 일 최대 3개("• " 접두 문자열). 배지는 이
  앱 고유의 옅은 톤(15% 불투명도) 유지 — 진하고 꽉 찬 배지로 바꾸지 말 것
  (앱 UI 통일성 유지 요청).
- 근무·할일 계산은 위젯 2와 같은 함수 재사용(getEffectiveShiftName/
  getShiftColor + getRepeatTodosForDate/getEventsForDate), buildSchedulePayload()
  → pushScheduleToWidget(). payload: { headers, satCol/sunCol,
  pages: [{days(14)}]×3 } — days는
  {date,dayNum,isToday,shiftName,color,batchId,holidayName,todos}
  (batchId/holidayName은 2026-07-18 추가).
- **일괄수정만 이어붙이기 + 공휴일 이름 표시**: 위젯 2와 완전히 같은 규칙
  (판단 기준·줄 경계 처리·SpannableString 방식 전부 동일, 자세한 이유는
  위젯 2 항목 참고) — **이 위젯은 원래 이 구분 자체가 아예 없어서 "괌"처럼
  여러 날을 한 번에 바꾼 근무가 모든 칸에 그대로 반복 표시되는 문제가
  있었음**(위젯 2는 이름 기준으로나마 병합 로직이 있었는데 이 위젯엔
  그것도 없었음) — 2026-07-18에 위젯 2와 같은 수준으로 맞춤.
- **페이지 넘기기**: [지난 2주, 이번 2주, 다음 2주] 3페이지
  (SCHEDULE_PAGE_OFFSET_DAYS=[-14,0,14], 인덱스 1이 항상 "이번"). 스와이프
  대신 자기 자신에게 보내는 브로드캐스트로 페이지 인덱스만 바꿈(RemoteViews
  스와이프 페이지는 AdapterViewFlipper+RemoteViewsService가 필요해 훨씬
  복잡해서 검토 후 기각).
- **탭 영역(최종)**: 날짜 숫자(sch_date_N)·근무 배지(sch_shift_N)는 항상
  팝업(아래 항목, 어느 열이든 예외 없음), 그 둘을 뺀 칸의 나머지(할 일
  줄)는 **맨 왼쪽 열(0)에서 왼쪽 넘기기, 맨 오른쪽 열(6)에서 오른쪽 넘기기,
  가운데 열(1~5)에서는 그냥 팝업** — 위/아래 두 줄 모두 완전히 동일한
  규칙(요일 이름이 아니라 열 위치 기준, weekStart와 무관 — 처음엔 "월요일/
  일요일 칸"처럼 요일 이름 기준이었지만 최종적으로 위치 기준으로 정리됨,
  다시 요일 이름 기준으로 되돌리지 말 것). 날짜 숫자·근무 배지가
  sch_cell_N(칸 전체) 안에 중첩된 자식 뷰라, 위 "중첩 클릭 영역" 규칙대로
  그 둘 위는 자식(팝업)이 받고 나머지는 부모(cellId, 열 위치에 따라 팝업
  또는 넘기기)가 받음. 새 팝업 대상 뷰가 필요해지면 그 자식 뷰에
  dayPending을 직접 걸 것.
- **날짜 칸 팝업**: 위 세 영역(넘기기 대상인 맨 왼쪽/오른쪽 열의 할 일 줄
  제외) 중 아무 곳이나 누르면 DayQuickViewActivity(QuickAddDialogTheme +
  quick_add_bg.xml 재사용)가 뜸 — 위젯이 이미 그리고 있던 그 날짜의 근무·할
  일을 그대로 인텐트 extras로 받아 보여주기만 함(체크·수정 불가, 보기
  전용, 따로 다시 계산 안 함). 입력칸(EditText, 엔터=추가)에 쓴 항목은
  **미배치가 아니라 그 날짜에 바로 배치된 한 번짜리 할 일**로 들어감 —
  "pending_dated_items"({text,date} 객체 배열) →
  WidgetBridgePlugin.getPendingDatedItems()/clearPendingDatedItems() → JS의
  syncWidgetDatedItems()(앱 시작 + 포그라운드 복귀 시)가
  placeInboxItem()과 같은 모양의 진짜 이벤트(state.events, date/endDate
  둘 다 그 날짜)로 만듦.
- 각 날짜 칸의 PendingIntent는 requestCode(200+i) + action("com.hyeongju.
  routineapp.OPEN_DAY_"+날짜)을 같이 줌 — 페이지를 넘겨 같은 칸 위치가
  다른 날짜를 가리키게 될 때의 충돌 방지(위 공통 규칙).

## 위젯 4 — 오늘 전체 관리 (TodayWidgetProvider / TodayWidgetService)
- 오늘 날짜·요일·근무를 위젯 2·3처럼 크게 보여주고, 그 아래 오늘의 반복
  할일 + 한 번짜리 할 일을 스크롤 가능한 목록으로 보여줌. 목록 줄을 탭하면
  위젯 안에서 바로 완료 체크가 토글됨(낙관적 갱신) — 컬렉션
  위젯(RemoteViewsService+RemoteViewsFactory, ListView)을 이 앱에서 처음
  쓴 위젯.
- **체크 반응 영역은 체크칸(item_check)만** — 텍스트(item_text)와 나머지
  여백은 대신 "오늘 탭 화면 열기"로 분리돼 있음(각 뷰마다 자기 몫의
  fillInIntent가 필요, 위 "중첩 클릭 영역" 규칙 — 컬렉션 위젯은 부모→자식
  자동 상속이 안 됨). 항목이 위젯 칸을 다 못 채울 때도 그 아래 빈자리가
  눌리도록 "보이지 않는 빈 줄"을 실제 항목 뒤에 덧붙임(할 일이 정말 0개일
  때는 안 붙임 — today_empty 문구가 정확히 0개일 때만 뜨는 방식이라
  건드리면 그 문구가 안 뜨게 됨).
  **몇 개를 붙일지(2026-07-16 세 차례 조정 끝에 최종 확정 — FILLER_COUNT=1,
  고정값)**: 처음엔 고정 12개를 붙였는데, 항목이 몇 개 없을 때 위젯
  높이보다 빈 줄이 훨씬 많아져서 할 일이 거의 없어도 목록이 "당겨서
  스크롤"되는 상태가 돼버렸음. 그다음 AppWidgetManager.getAppWidgetOptions()
  로 위젯의 실제 높이(dp)를 읽어서 "화면을 딱 채울 만큼만" 계산하도록
  바꿨는데, 그 어림값이 실제 기기에서 많이 틀려서(항목 3개짜리 목록인데도
  드래그하면 화면 하나 가득 빈 공간이 나옴, 사용자가 스크린샷으로 확인)
  여전히 필요 이상으로 크게 드래그됐음. **결국 위젯 크기를 재려는 시도를
  접고, 실제 항목 바로 아래 딱 한 줄(FILLER_COUNT=1)만 붙이는 가장 단순한
  방식으로 되돌림** — 그 이상은 절대 안 늘어나서 항목이 적을 때 크게
  드래그되는 일이 없음(대신 위젯이 아주 클 때 목록 훨씬 아래쪽의 빈
  공간까지는 안 눌릴 수 있음 — 드래그 방지를 우선한 의도된 트레이드오프).
  항목이 위젯을 이미 다 채우고도 남을 만큼 많으면 그건 원래도 스크롤이
  필요한 정상적인 경우라 문제없음. **위젯 크기를 정확히 재서 빈 줄 개수를
  동적으로 맞추는 방식은 이미 실패했으니 다시 시도하지 말 것** — 고정값
  1이 최종 결정.
  **버그(수정 완료, 2026-07-16)**: 다크 모드에서 체크칸이 종종 안 보이던
  문제 — 빈 줄은 item_check를 INVISIBLE로 숨기는데, 실제 항목 줄의
  getViewAt()은 아이콘 이미지만 바꿔 끼울 뿐 visibility를 한 번도 다시
  VISIBLE로 안 돌려놓고 있었음. 같은 레이아웃(getViewTypeCount()=1)을 쓰는
  줄끼리 안드로이드가 뷰를 재활용할 때 이 상태가 그대로 남아서, 실제 항목
  줄인데도 체크칸이 안 보이는 경우가 생겼음 — 실제 항목 쪽에도
  `setViewVisibility(item_check, VISIBLE)`을 명시적으로 추가해서 고침.
- 체크하면 항목이 목록에서 바로 사라짐(취소선으로 남기지 않음 — "체크하면
  안 보이게" 요청, 계속 보여주는 방식으로 되돌리지 말 것). handleToggle()이
  저장된 today_widget_data에서 그 항목을 즉시 통째로 제거하고, "이 항목을
  이 상태로 만들어라"(토글이 아니라 최종 상태)를
  today_widget_pending_toggles에 쌓음(임시 우편함 패턴). JS의
  syncWidgetTodayToggles()가 앱 시작·포그라운드 복귀 시 반영.
- 근무 배지는 둥근 밑그림(today_shift_bg, RelativeLayout으로 today_shift에
  정렬) + setColorFilter 패턴(공통 규칙의 "RemoteViews가 못 그리는 것"
  참고).
- **맨 아래 + 버튼**: 목록 자리 중 아래 56dp를 큰 "+" 글자(today_add_button,
  #007AFF)로 채움(목록·"+" 버튼이 같은 부모의 형제라, 이 고정 높이 뷰를
  더하면 weight=1인 목록 칸이 자동으로 그만큼만 줄어듦). 누르면
  **TodayQuickAddActivity**(QuickAddActivity와 완전히 같은 입력칸 하나뿐인
  화면, activity_quick_add.xml 재사용 — 날짜·근무·할일 목록을 같이 보여주는
  화면(DayQuickViewActivity 재사용)을 먼저 시도했다가 "다른 거 보여주지
  말고 할일추가 위젯처럼만" 요청으로 지금 화면으로 바뀜, 되돌리지 말 것)이
  뜸. 저장 위치는 미배치가 아니라 **오늘 날짜** — pending_dated_items를
  스케줄 위젯과 그대로 공유해서 씀(TodayQuickAddActivity가
  DayQuickViewActivity.PREFS_NAME/KEY_PENDING_DATED_ITEMS 상수를 그대로
  참조, 새 임시 저장소를 안 만듦). 위젯이 아직 데이터를 못 받았을 때는
  기기의 오늘 날짜를 SimpleDateFormat("yyyy-MM-dd")으로 직접 계산해 대신
  씀(TodayWidgetProvider·TodayQuickAddActivity 둘 다 같은 fallback을 각자
  가짐 — 근무 로직이 아닌 단순 사실이라 네이티브 계산 허용). 이 버튼의
  PendingIntent는 requestCode 2 + action에 "_FROM_TODAY_WIDGET" 접미사를
  붙여서 스케줄 위젯의 DayQuickViewActivity 인텐트와 안 겹치게 함. 위젯 1과
  같은 이유로 NEW_TASK|MULTIPLE_TASK 플래그도 줌.
- **크기 2x2 기본 + 자유 리사이즈**(2x2~4x4 이상) — 사용자가 명시적으로
  요청함, 최소 크기를 250dp 근처로 올리거나 기본 크기를 4x4로 키우는 방향
  으로 되돌리지 말 것. today_date에 maxLines="1"/ellipsize="end"로 좁은
  너비에서도 안 깨지게 해둠.

## 위젯 5 — 미배치 목록 (InboxWidgetProvider / InboxWidgetService)
- 미배치(인박스) 목록을 그대로 보여줌 — 근무·날짜 계산이 아예 없어서 다섯
  위젯 중 가장 단순. buildInboxWidgetPayload()가 state.inbox에서
  targetMonth로 태그된 항목만 뺀 목록(count, items)을 만들어 넘김 — 이
  필터링은 앱의 기본 미배치 목록 화면(연간 탭에서 그 달을 안 보고 있을
  때)과 똑같은 기준(!it.targetMonth). targetMonth 태그된 항목까지 보여주려
  하지 말 것(지저분해지지 않게 하려는 원래 설계 의도와 어긋남).
- **목록 자체는 읽기 전용**(항목 탭 = 앱 열기, 어떤 항목을 눌렀는지는 구분
  안 함). 컬렉션 위젯이라 빈 fillInIntent라도 각 줄에 있어야 탭에 반응함
  (위 공통 규칙) — 여기선 내용이 비어있는 fillInIntent를 각 줄에 붙이고
  PendingIntentTemplate 자체를 "앱 열기" 액티비티 인텐트로 씀(이 템플릿용
  PendingIntent만 FLAG_MUTABLE, fillInIntent 병합 자체가 요구하는 안드로이드
  제약).
- **맨 아래 + 버튼(이 위젯의 유일한 "쓰기")**: 위젯 4와 같은 자리 배치(아래
  56dp, "+" 글자, inbox_add_button, #007AFF)지만 **QuickAddActivity를 그대로
  재사용**(미배치용 임시 우편함 pending_inbox_items) — 새 화면을 안 만들고
  위젯 1과 완전히 같은 경로를 씀. requestCode 2 + 별도 action으로 위젯 1의
  QuickAddActivity 인텐트(requestCode 0)와 구분(위 공통 규칙).
- 위젯 4와 완전히 같은 구조(RemoteViewsService + RemoteViewsFactory로
  목록을 채움, Provider는 헤더만) — 다만 체크·임시 보관함 관련 코드가 전혀
  없어서 두 파일 다 훨씬 짧음.
- **크기 2x2 기본 + 자유 리사이즈**(위젯 4와 같은 이유).

## 위젯 선택 화면 미리보기 (홈 화면에서 위젯 추가할 때 뜨는 목록)
- 각 위젯을 등록하는 `<receiver>` 태그의 `android:label`이 픽커에 보이는
  이름(앱 자체 라벨이 아님) — 할일추가="할 일 추가", 달력="달력",
  스케줄="스케줄"("일정"이라는 표현은 이 프로젝트에서 안 쓰기로 했으므로
  위젯 이름에도 그대로 적용, 아래 "용어" 참고), 오늘 할일="오늘 할일",
  미배치="미배치 목록"(위젯 안 제목 inbox_title과 항상 같이 맞출 것 — 픽커
  라벨과 위젯 안 제목이 다르면 헷갈림).
- 미리보기는 두 방식을 같이 씀: `previewLayout`(API 31+ 전용 — 실제 위젯이
  쓰는 살아있는 레이아웃을 그대로 재사용하면 텍스트가 전부 빈 문자열로
  보임, 그래서 예시 데이터를 하드코딩한 별도 `*_preview.xml` 레이아웃을
  따로 둠) + `previewImage`(모든 버전 지원, res/drawable-nodpi/
  widget_preview_*.png 정적 이미지 — 실기기/에뮬레이터 없이 예시 사진으로
  대체하는 것을 사용자가 명시적으로 허락함). 할일추가 위젯은 텍스트가
  XML에 이미 하드코딩돼 있어서 별도 preview 레이아웃 없이 실제 레이아웃을
  그대로 씀.
- PNG는 위젯 디자인이 실제로 바뀔 때마다(previewLayout과 달리 자동으로 안
  바뀜) 다시 만들어야 함 — Node.js `sharp`로 SVG를 그리는 방법과 headless
  Chrome으로 HTML을 스크린샷하는 방법 둘 다 써봤음, 방법은 상관없고 실제
  위젯 모습과 맞춰두는 게 중요.

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
1. **계획 승인은 큰 변화이거나 오류 위험이 있다고 판단될 때만 먼저 받을
   것**(2026-07-16 확정) — 그 정도가 아닌 일반적인 수정(기능 추가·버그
   수정 등 평소 작업 범위)은 계획을 먼저 안 물어보고 스스로 판단해서 바로
   진행. 어떤 파일을 고칠지도 사용자가 일일이 짚어주지 않아도 스스로
   판단할 것(요청 내용과 이 문서의 원칙에 맞게, 사용자 스타일대로). 계획을
   물어봐야 할 정도로 크거나 위험한 경우엔, 설명은 프로그래밍 용어를 빼고
   쉬운 말로 할 것 — 승인받으면 그 뒤로는 세부 진행마다 다시 묻지 말고
   끝까지 처리할 것.
2. 데이터 구조 변경 시 기존 localStorage 보존, 마이그레이션 필수.
3. 수정 후 셀프체크하고 한국어로 요약 보고.
4. 모든 응답 한국어.
5. 새 기능·화면은 '핵심 시스템' 흐름과 '경계 기준'에 맞는지 먼저 검토.
   어긋나면 만들기 전에 사용자에게 알릴 것.
6. 사용자에게 하는 모든 말은 전문용어 없이 요점만 간단히, 일반인에게 설명하듯
   쉽게 할 것(계획 설명뿐 아니라 진행 상황·완료 보고 등 모든 대화에 적용).
   파일명·함수명·명령어 같은 코드 용어는 꼭 필요할 때가 아니면 언급하지 말고,
   "무엇을 왜 했는지"만 짧게 전달할 것.
7. 안드로이드 빌드·배포는 코드 수정할 때마다 바로 하지 않고 모아둠 —
   사용자가 "빌드해줘"라고 명시적으로 말하기 전까지는 빌드·배포 여부를
   먼저 묻지도 말 것. **"빌드해줘"라는 말 자체가 안드로이드 빌드 +
   GitHub Pages 배포까지 한 번에 끝내라는 뜻**(위 "배포" 섹션 참고) — 빌드만
   하고 배포를 빼먹거나, 둘 중 하나만 하고 다시 확인받으려 하지 말 것.
8. **달력(근무·공휴일 등) 관련 UI를 고칠 때는 달력 페이지(웹)와 위젯(달력
   위젯·스케줄 위젯 등)을 하나로 보고 항상 같이 관리할 것**(2026-07-18
   확정, 사용자가 스케줄 위젯에서만 일괄수정 표시가 깨진 걸 발견하고 명시적
   요청 — "앞으로 달력에서 표현하는 UI를 수정할 때는 달력페이지와 위젯을
   하나라고 생각하고 통합해서 관리해"). 한쪽만 고치고 위젯(또는 웹)에는
   안 반영하는 일이 없게 할 것 — 예: 근무 이어붙이기/일괄수정 구분 로직
   (batchId), 공휴일 표시가 이 원칙으로 위젯까지 같이 반영된 사례. 다만
   사용자가 "이건 웹만"/"이건 위젯만"처럼 범위를 명시하면 그 말을 따름
   (이 원칙은 명시적으로 안 좁혀졌을 때의 기본값).

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
    - 저장: 화면에서 뭔가 바뀔 때마다(텍스트 입력, 근무·요일
      칸 클릭, 기간/음력 스피너를 놓을 때, 모드 전환) syncPromoteEditorToState()가
      바로 저장. 새로 만들기 중엔 텍스트가 비어 있거나 규칙을 아직 하나도
      못 만들 상태(예: 모드 자체를 안 골랐거나 칸을 하나도 안 켬)면 저장을
      보류하고 조용히 기다림 — 텍스트+유효한 규칙이 갖춰지는 순간 그때
      state.events에 항목이 생김. 닫기는 배경(바깥) 탭으로만.
    - **"기간"/"근무·요일" 탭 다시 누르면 선택 취소(2026-07-16 추가, 같은 날
      두 번째 요청으로 동작이 한 번 더 갈림)**: 이미 켜져 있는 모드 버튼을
      한 번 더 누르면 두 경우로 나뉨 —
      **새로 만드는 중**(!repeatEditorTargetEvent)이면 그냥 선택만 풀림
      (deselectMode() → setPromoteMode(null), 양쪽 버튼 다 꺼지고 두 섹션
      다 숨겨짐 → syncPromoteEditorToState()는 규칙 없음 상태로 조용히
      저장을 보류하는 기존 동작을 그대로 씀, 별도 "취소" 로직 없음).
      **이미 저장된 반복 할일을 편집하는 중**(repeatEditorTargetEvent 있음)
      이면 confirmDeleteEditingRepeatEvent()가 불려서 `confirm()` 안내창으로
      "반복 방식을 선택 해제하면 이 반복 할일이 삭제돼요. 삭제할까요?"를
      먼저 확인한 뒤, 확인하면 그 반복 할일을 통째로 삭제("반복 할일" 탭
      카드를 스와이프해서 지우는 것과 완전히 같은 삭제 경로: state.events에서
      filter로 제거 → saveState → closeRepeatEditor → renderRoutine/renderGrid/
      renderToday/(날짜 상세 열려있으면)renderDDTodos) — 반복 규칙이 없는
      반복 할일은 데이터 구조상 존재할 수 없어서 "선택만 풀기"가 불가능하므로
      삭제로 처리함(처음엔 "편집 중엔 아무 일도 안 일어남"으로 만들었다가,
      같은 날 사용자가 "안내문구 띄우고 지워달라"고 재요청해서 바뀜 — 다시
      무동작으로 되돌리지 말 것).
    - **"근무·요일" 탭 처음 누르면 원본 날짜 칸 자동 체크(2026-07-17
      추가)**: "반복으로 전환" 중(repeatEditorSourceEvent 있음)이고 표에서
      아직 아무 칸도 안 골랐으면(promoteShiftGridSelected.size === 0),
      "근무·요일" 버튼을 누르는 순간 원래 그 할 일의 날짜가 어떤 근무·요일
      이었는지 계산(dayIndex0ForDate + getEffectiveShiftName로 근무 이름,
      parseLocalDate(...).getDay()로 요일 번호)해서 그 칸 하나를 미리 체크해줌
      — 매번 표에서 처음부터 다 찾아 눌러야 하는 수고를 덜기 위함. **새로
      만들기(원본 날짜 없음)이거나, 이미 칸을 하나라도 고른 적 있으면(예:
      "기간"으로 갔다가 "근무·요일"로 되돌아온 경우) 안 건드림** — 사용자가
      이미 손댄 선택을 덮어쓰지 않기 위한 조건. renderShiftMatrix()를 다시
      불러서 화면에도 바로 반영함(matrix가 openRepeatEditor 시점에 이미 한 번
      그려져 있어서, 그 이후 promoteShiftGridSelected를 바꾸면 다시 그려줘야
      화면에 체크 표시가 보임).
    - **"근무·요일" 반복 중복 감지 + 합치기(2026-07-17 추가,
      attemptCloseRepeatEditor)**: "반복으로 전환" 중일 때만(repeatEditorSourceEvent
      있음, "새로 만들기"는 대상 아님 — 사용자가 명시적으로 확정) 적용,
      "근무·요일"(type:'shift') 방식일 때만("기간"/음력은 절대 대상 아님 —
      기간-근무든 기간-기간이든 항상 별개 할일로 관리하기로 확정, 다시 이
      확인을 기간 쪽에 넣지 말 것). **확인 타이밍은 칸을 고르는 도중이
      아니라 화면을 닫으려는 순간**(2026-07-17, 같은 날 재조정 — 처음엔
      syncPromoteEditorToState의 생성 분기에서 칸 하나 고르자마자 바로
      물어봤는데, 사용자가 "선택하자마자가 아니라 선택을 다 하고 저장되기
      직전 단계에서" 물어보라고 타이밍을 직접 지정해서 옮김). 이 화면은
      확인 버튼이 없고 배경(바깥) 탭이 유일한 닫기 통로라(위 "닫기 방식"
      참고), 그 배경 탭 리스너가 closeRepeatEditor() 대신
      attemptCloseRepeatEditor()를 부르도록 바꿈 — 이 함수가:
      1. 지금 만들고 있던 repeatEditorTargetEvent가 있고 그 규칙이
         type:'shift'면, 같은 텍스트의 다른 type:'shift' 반복 할일이 이미
         있는지 찾음(자기 자신은 제외).
      2. 있으면 `confirm('이미 반복 할일에 "..."이(가) 있어요. 하나로
         합칠까요?')`.
         - **"예"**: 두 항목의 근무×요일 칸을 합집합으로 합쳐서 기존
           항목의 `.repeat`을 다시 계산(기존 항목의 텍스트·카테고리·중요
           표시는 그대로 유지, 새로 전환하던 쪽 값으로 안 덮음) → 방금
           세션에서 만들었던 새 항목(ev)은 state.events에서 지움(어차피
           자동저장으로 이미 만들어져 있던 것이라 지워서 정리) → 전환 중이던
           원본(한 번짜리) 기록의 날짜를 기존(합쳐진) 반복 할일의
           excludedDates에 추가(기존 "반복 전환 시 원본 기록 보존" 규칙과
           동일한 방식, 대상만 기존 항목).
         - **"아니요"**: 방금 세션에서 만든 항목은 이미 자동저장으로
           만들어져 있으므로 그대로 두고 아무 것도 안 함(별개 반복 할일로
           확정).
      3. 마지막에 항상 closeRepeatEditor()를 불러 실제로 닫음.
      **"삭제 후 닫기"(confirmDeleteEditingRepeatEvent)는 여전히
      closeRepeatEditor()를 직접 씀** — attemptCloseRepeatEditor를 거치지
      않음(이미 지운 항목을 다시 합칠 이유가 없음, 혼동해서 바꾸지 말 것).
    - 삭제: 이 화면 안에 버튼 없음. "반복 할일" 탭 목록(renderRoutine)에서
      한 번짜리/오늘 탭 할 일과 똑같이 왼쪽으로 스와이프해서 지움
      (wrapWithSwipeToDelete 재사용). 여기서 스와이프는 그 반복 항목을
      통째로 지우는 것 — 오늘 탭/날짜 상세에서 스와이프하면 그 날짜만
      제외(excludedDates)하는 것과는 다르니 혼동 금지(아래 excludedDates
      설명 참고, 예전에 실제로 헷갈려서 버그 신고된 적 있음).
    - **중요(★) 토글은 없음(2026-07-16 — 지웠다가 "이상하다"는 반응에 일단
      되돌렸다가, 사용자가 정확히 "중요표시 체크만" 지워달라고 다시 확인해서
      최종적으로 없앰)**: 처음엔 이 토글과 promote-source-date-info(제목 옆
      "7월 16일 (수) 주간" 같은 원본 정보 줄)를 같이 지웠다가, 의도와 다르게
      구현된 것 같다는 반응으로 둘 다 되돌렸음 — 재확인해보니 실제로 지우고
      싶었던 건 중요 토글 하나뿐이었고, 원본 정보 줄(promote-source-date-info)은
      그대로 두는 게 맞았음. **그래서 지금은 중요 토글만 없고, 원본 정보 줄은
      여전히 있음** — 다시 원본 정보 줄까지 같이 지우지 말 것(과거에 그렇게
      했다가 되돌린 이력 있음). 중요 표시가 필요하면 만든 뒤 "반복 할일" 탭
      목록에서 그 줄을 눌러 나오는 메뉴(반복/중요/수정)의 "중요"로 하면 됨 —
      이 화면에 다시 별표 버튼을 만들지 말 것. 내부적으로 promoteImportant
      변수는 남겨뒀는데, 이미 중요 표시가 있던 반복 할일을 이 화면에서 편집해도
      그 표시가 조용히 사라지지 않게 하려는 용도일 뿐(읽기만 함).
    - **"반복으로 전환"해도 원래 한 번짜리 기록은 안 지움(2026-07-16
      변경)**: 예전엔 syncPromoteEditorToState()가 새 반복 항목을 만든 뒤
      repeatEditorSourceEvent(전환 중인 원래 한 번짜리 할 일)를 state.events
      에서 지웠는데, 그러면 새 반복 규칙이 원래 날짜와 안 맞을 때(예: 원래는
      수요일 날짜였는데 "화요일마다"로 바꾸면) 달력에서 그 원래 날짜의
      기록이 통째로 사라져 보이는 문제가 있었음 — 사용자가 "반복으로 돌려도
      달력에는 그대로 남겨달라"고 요청해서, 이 삭제 줄 자체를 없앰. **이제는
      원래 한 번짜리 항목은 손대지 않고 그대로 남고, 새 반복 규칙 항목이
      완전히 별개로 하나 더 생김** — 사용자가 직접 고른 방식(반대안은 "반복
      규칙이 원래 날짜도 반드시 포함하도록 자동 보정"이었는데, 그건 안
      씀). **원래 날짜 중복 표시 고침(2026-07-16, 같은 날 재수정)**: 처음엔
      "새 반복 규칙이 우연히 원래 그 날짜에도 맞아떨어지면 한 번짜리 기록과
      반복 항목이 같이 보여서 중복돼 보일 수 있음"을 의도된 트레이드오프로
      남겨뒀는데, 사용자가 실제로 이걸 겪고 오류로 신고함 — 새로 만드는
      반복 항목의 excludedDates(오늘 탭에서 반복 할일을 왼쪽으로 쓸어서
      "이 날짜만 빼기"할 때 쓰는 것과 완전히 같은 필드)에 원래 날짜를 처음부터
      넣어서 고침: `repeatEditorTargetEvent.excludedDates = [repeatEditorSourceEvent.date]`
      (target을 새로 만드는 분기에서만, 최초 전환 시 한 번). getRepeatTodosForDate()가
      규칙 종류(shift/interval/lunar) 상관없이 excludedDates부터 먼저 걸러내므로
      모든 반복 방식에서 동일하게 적용됨 — 그 날짜는 반복 쪽에서 빠지고
      원래 한 번짜리 기록만 보임, 다른 날짜는 영향 없음.
  - **"반복 할일" 탭 → "반복·중요" 탭으로 확장(2026-07-17)**: 하단 탭 이름이
    "반복·중요"로 바뀌고(nav-routine 라벨만 변경, id/switchTab 키는 여전히
    'routine' 그대로 — 코드 쪽 이름은 안 바꿈), 화면 위쪽에 필터 버튼 두 개
    (routine-filter-repeat/-important, #routine-filter-row)가 생김.
    **"기간"/"근무·요일" 모드 버튼과 같은 배타적 단일 선택**(routineFilterMode:
    'repeat'|'important', 기본값 'repeat')**이지, 독립 토글이 아님** — 처음엔
    둘 다 동시에 켤 수 있는 토글로 만들었다가, 사용자가 "둘 중 하나만
    눌리는 걸로" 요청해서 항상 정확히 하나만 활성 상태인 방식으로 바꿈(다시
    동시에 두 개 켜지는 방식으로 되돌리지 말 것). 한 번짜리(반복 아닌) 중요
    할일은 '중요' 모드에서 이 앱의 "앞으로만 본다" 원칙대로 **오늘 이후
    것만** 포함(지난 날짜는 안 보임).
  - renderRoutine()은 routineFilterMode에 따라 완전히 다른 목록을 그림 —
    **'반복' 모드**는 예전 "반복 할일" 탭과 완전히 같음(중요 여부 상관없이
    반복 규칙이 있는 항목 전부, ROUTINE_GROUPS로 근무·요일 → 기간 순 묶음,
    repeatGroupKey(rule)이 분류, buildRoutineCard로 그림 — 카드+화살표,
    누르면 openRepeatEditor로 규칙 편집). **'중요' 모드**는 반복이든
    한 번짜리든 상관없이 item.important인 것만 모아서 보여줌 — **한 번짜리
    (반복 아닌) 것을 먼저, 반복인 것을 그 뒤에**(2026-07-17, 처음엔 반복을
    먼저 보여줬다가 사용자 요청으로 순서를 바꿈 — 다시 반복을 앞으로
    되돌리지 말 것) 섹션으로 나눠서: "한 번짜리" 섹션은 날짜 이른 순으로
    정렬해서 buildTodoRow(다른 화면과 완전히 같은 줄 모양 — 텍스트를 누르면
    반복/중요/수정 메뉴, 스와이프하면 그 할 일 자체가 삭제됨, excludedDates
    개념 없음)로, "반복" 섹션은 buildRoutineCard로 그림. 공용 헬퍼
    (appendRoutineSectionLabel/appendRoutineCardRow/appendRoutineTodoRows)로
    두 모드가 그리는 코드를 공유 — 새 마크업을 만들지 않고 기존 두 렌더러를
    그대로 재사용한 것.
  - **buildRoutineCard: "근무·요일" 카드는 조건 문장 대신 이름을 눌러 펼치는
    표(2026-07-17 추가)**: 예전엔 이름 밑에 항상 describeRepeatRule()이 만든
    조건 문장(예: "주간인 화요일마다")이 보였는데, "근무·요일"(type:'shift')
    방식 카드에서는 이 문장을 없애고 대신 **이름(routine-name)을 누르면 그
    자리에 실제 근무×요일 표가 펼쳐짐**(다시 누르면 접힘) — buildShiftMatrixPreview(ev)
    가 편집 화면의 표(.promote-shift-matrix)와 같은 모양으로 보기 전용
    (칸이 `<div>`, 클릭 핸들러 없음)으로 그림. 편집 화면이 아직 한 번도 안
    열린 상태에서도 동작해야 해서, 편집 화면 전역(promoteShiftAllNames)에
    기대지 않는 별도 헬퍼(computeAllShiftNames/gridSelectionForShiftRule)를
    씀. 이름 탭은 `e.stopPropagation()`으로 카드 전체 탭(→ openRepeatEditor로
    편집 화면 열기)과 분리됨 — 이름 말고 카드의 나머지 부분(화살표 포함)을
    누르면 여전히 편집 화면이 열림. **"기간" 등 그 밖의 방식은 그대로**
    (조건 문장 계속 보임, 이름 눌러도 아무 반응 없음 — 표가 없는 방식이라
    대상 아님).
  - excludedDates?: [dateStr,...] — 오늘 탭/날짜 상세에서 반복 할일을 왼쪽으로
    쓸어 삭제하면 그 날짜만 여기 추가됨(반복 할일 자체는 안 지워짐).
    반복 할일 전체 삭제는 반드시 "반복 할일" 탭에서만(openRepeatEditor의
    삭제 버튼). 이 구분 헷갈리지 말 것 — 실제로 헷갈려서 버그 신고된 적 있음.
  - 화면에 그릴 때는 반드시 buildTodoRow()/renderTodoList() 공용 함수로 그릴 것.
    반복 할일과 한 번짜리 할 일은 겉모습이 완전히 같아야 함 — 별도 마크업/
    스타일을 새로 만들지 말 것. 오늘 탭과 날짜 상세도 겉모습·조작 방식이
    완전히 같음 — 화면마다 다르게 만들지 말 것.
  - **할 일 줄 조작 방식**: 줄에 상시로 붙어있는 버튼 없이, 텍스트를 누르면
    뜨는 작은 메뉴(반복/중요/수정 3개, 카테고리 고르기 팝업과 같은
    cat-picker-menu 스타일 재사용, 취소 버튼 없이 바깥 탭하면 닫힘 —
    openTodoItemMenu 함수, HTML의 #todo-item-menu)로 조작함. 텍스트 수정도
    이 메뉴의 "수정"을 거침(startInlineTextEdit — 기간(멀티데이) 할일은
    수정을 누르면 openPeriodEdit). 다시 버튼들을 줄에 상시로 붙이거나 텍스트
    탭으로 바로 수정되게 만들지 말 것.
    **"반복" 항목은 두 갈래로 동작**(2026-07-16 — 원래는 한 번짜리 할 일에만
    있었는데, "이미 반복인 할일에도 반복 매뉴가 뜨게 해달라" 요청으로 넓힘):
    한 번짜리(kind==='onetime', 기간/멀티데이 아님)면 `openRepeatEditor({
    sourceEvent: item })`(반복으로 전환, 원래 있던 동작). 이미 반복인
    항목(kind==='repeat')이면 `openRepeatEditor({ editEvent: item })`(그
    반복 규칙을 바로 고치는 편집 화면 — "반복 할일" 탭에서 카드를 눌렀을 때
    (buildRoutineCard)와 완전히 같은 경로를 재사용한 것, 새로 만들지 않음).
    기간(멀티데이) 할일에는 여전히 반복 항목 자체가 없음(원래도 대상 아니었음).
    **"중요" 항목만 눌러도 메뉴가 안 닫힘**(2026-07-16, 사용자 요청 —
    "반복"/"수정"은 그대로 누르면 닫힘, "중요"만 예외) — 대신 버튼 글자를
    그 자리에서 바로 갱신(★ 중요 해제 ↔ ☆ 중요 표시)해서 눌린 결과가 바로
    보이게 함. refreshFn()으로 뒤의 목록도 같이 갱신되지만 #todo-item-menu는
    목록과 분리된 별도 요소라 목록이 다시 그려져도 메뉴는 안 사라짐. 바깥을
    탭하면 여전히 닫힘(document 클릭 리스너는 그대로 유지).
  - **드래그**: 손잡이 없이 카드(줄) 자체를 꾹 눌러서 드래그함.
    wrapWithSwipeToDelete(rowEl, bgVar, onDelete, dragConfig)가 스와이프
    삭제와 드래그를 하나의 상태 기계로 처리(빠르게 움직이면 스와이프, 가만히
    500ms 누르고 있으면 드래그). dragConfig(선택, 없으면 스와이프만 —
    "반복 할일" 탭 카드는 드래그 필요 없어서 안 줌): { eventId, refreshFn,
    enableInboxDrop, rootEl(선택) } — enableInboxDrop이면 하단 미배치 버튼
    위에 놓았을 때 unplaceEventToInbox 실행, rootEl이 있으면(오늘 탭만) 그
    안의 .timeline-section-list로 옮겨서 시간대/순서 변경까지 같이 됨(날짜
    상세는 시간대 구역이 없어서 rootEl 자체가 없음).
  - timeSlot?: 'morning'|'afternoon'|'night' — 오늘 탭 타임라인 구역. 없으면
    "미정"으로 취급(값이 'unset'으로 저장되지는 않음, 그냥 필드 자체가 없는
    상태 = 미정). 카테고리·important처럼 그 할 일 객체 자체에 저장되는 정보라
    반복 할일은 매번 같은 구역에 나타남.
  - timelineOrder?: number — 같은 timeSlot(미정 포함) 안에서의 순서. 없으면 0
    취급. 오늘 탭에서 카드를 꾹 눌러 드래그하면 setupTimelineDrag()가 놓인
    구역의 timeSlot과, 그 구역 안 최종 DOM 순서 기준으로 형제 항목들의
    timelineOrder를 전부 10 간격으로 재계산해서 저장.
- inbox[]: 미배치 { id, text, createdAt, targetMonth? }
  - targetMonth?: "YYYY-MM" — 연간 탭에서 월 박스로 드래그해서 "대략 이 달에"로
    태그해 둔 것. 있어도 여전히 미배치 상태(세 번째 상태 아님, 위 핵심 시스템
    참고). setupInboxItemDrag()가 롱프레스 드래그를, renderInboxList()가
    현재 보고 있는 달(calYear/calMonth, tab-month 활성 시)에 맞는 targetMonth
    항목만 상단에 별도 표시하는 로직을 담당.
  (반복 배치 옵션은 아직 없음, 범위 밖으로 보류 중)
- shiftOverrides: 날짜 예외{date,shiftName,batchId?} / D번호 예외{dayIndex,shiftName}
  - batchId(2026-07-18 추가): "근무 일괄 수정" 시트로 여러 날짜를 한 번에
    바꿀 때만 그 시트를 한 번 쓸 때마다 공통으로 붙는 값(genId('shiftbatch')) —
    날짜 상세의 "한번만"으로 하루씩 따로 바꾼 예외는 이 필드 자체가 없음.
    달력 탭(appendShiftIndicator)과 달력·스케줄 위젯이 "이 연속이 진짜
    일괄 수정인가"(괌여행처럼 첫날에만 글자를 쓰고 이어붙일지, 비번·비번
    처럼 매번 글자를 반복할지)를 판단하는 유일한 근거 — 이름이 같다는
    것만으로 판단하면 하루씩 따로 바꾼 날짜가 우연히 이름이 같을 때
    잘못 합쳐지는 버그가 있었음(2026-07-17에 이 방식으로 확정, 그 전엔
    이름 기준 → 잘못 병합 → batchId 기준으로 두 번 수정됨 — 다시 이름
    기준으로 되돌리지 말 것).
- shiftColors { 근무이름: 색 }, weekStart
  - **근무 편집(설정 탭, 구 "근무유형 색")**: 이 값의 키 목록 + 기본
    패턴(state.days)·shiftOverrides·dayOverrides에 실제 쓰이는 이름을 합친
    게 getAllShiftTypeNames() — 설정 탭 "근무 편집" 화면과 날짜 상세
    "근무 변경" 팝업이 공유하는 "지금 있는 근무 이름 전체" 목록(단,
    "근무 변경" 팝업은 이보다 좁은 getShiftTypeNamesForPicker()를 씀 —
    아래 참고). 근무 편집 화면에서:
    - 이름을 누르면 그 자리에서 바로 수정(startShiftNameRename) — 이름이
      나오는 모든 곳(shiftOverrides/dayOverrides/shiftColors/shiftAlarms/
      반복 할일의 "근무·요일" 조건)을 한 번에 새 이름으로 옮김
      (renameShiftType).
    - "+ 근무 추가"로 새 근무를 만들면 프리셋 중 안 겹치는 색을 자동
      배정(pickUnusedShiftColorPreset).
    - 카드를 왼쪽으로 밀면 삭제(deleteShiftType) — **기본 교대 패턴에 있는
      이름(주간/야간/비번/휴무 등)은 삭제 대상이 아님**(설정 탭 "교대
      주기"가 관리하는 뼈대라 막고 경고만 보여줌, isBaseShiftPatternName).
      그 밖의 이름은 그 이름을 쓰던 날짜·자리 예외를 전부 지워서 원래
      근무표로 되돌림 — **그 이름이 실제로 쓰인 날짜가 있으면 삭제 전에
      꼭 확인창을 띄움**(2026-07-18 추가 — 여러 날짜에 걸친 걸 한 번에
      되돌리는 무거운 동작이라, 이 앱의 다른 삭제·덮어쓰기 동작들과 같은
      수준의 확인을 맞춤). 아무 날짜에도 안 쓰인 이름(예: "+ 근무 추가"로
      만들어놓고 실제로는 안 쓴 것)은 확인 없이 바로 지움.
  - **adhocShiftNames[]** (2026-07-18 추가): 날짜 상세 "근무 변경" 팝업의
    "+ 새 근무 만들기"나 "근무 일괄 수정" 시트에서 그 자리에서 즉석으로
    만든 근무 이름 목록 — 색은 shiftColors에 똑같이 저장되지만(달력 등에
    정상 표시됨), **설정 탭 "근무 편집" 목록에는 일부러 안 보임**(사용자
    요청 — 이런 즉석 근무는 관리 목록을 지저분하게 만들지 않게). "근무
    변경" 팝업 자체(getShiftTypeNamesForPicker, dayOverrides/기본 패턴/
    shiftColors만 보고 shiftOverrides는 안 봄)에서는 계속 다시 고를 수
    있음 — getAllShiftTypeNames()와 다른 이 좁은 목록 덕분에, "괌"처럼
    하루짜리로 바뀐 근무가 이 팝업 목록에 계속 남아 지저분해지는 것도
    같이 막음(2026-07-17 요청 — "한번만 바꾼 근무는 팝업창에 저장하지
    마").
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
- **달력 탭 근무 칸 모양(appendShiftIndicator, 2026-07-18 최종 확정 —
  네 차례 재조정 끝에 정리됨)**: 모든 근무 칸은 항상 모서리 없이 각지고
  칸 좌우 여백만큼 늘어나 옆 칸과 맞닿음(.cell-shift-band, 이름이 다른
  날끼리도 마찬가지로 맞닿음 — "스케줄·달력 위젯처럼 보이게 해달라"는
  요청). 이건 순전히 모양(항상 적용)이고, **글자를 실제로 찍을지는 완전히
  별개 기준** — 같은 batchId로 이어진 날짜(진짜 일괄 수정)만 그 구간 첫
  날에 한 번 쓰고 나머지는 비움, 그 밖의 모든 경우(기본 패턴, D번호 예외,
  하루씩 따로 바꾼 날짜)는 매 칸 이름을 그대로 반복. 색은 어디서든
  applyShiftBadgeColor(15% 옅은 배경 + 그 색 글자) 하나로 통일 — 예전엔
  이어붙인 칸만 꽉 찬 색+흰 글자로 따로 칠했었는데 통일함. **이 로직은
  달력·스케줄 위젯에도 그대로 있음**(위 "달력·위젯 통합 관리" 원칙,
  위젯 2·3 항목 참고) — 웹만 고치고 위젯을 빠뜨리지 말 것.
- **공휴일 표시**: getKoreanHoliday(dateStr)가 { name, isSubstitute } 반환
  (한국천문연구원 음력 데이터 기반, 대체공휴일 자동 계산). 색은
  --holiday-color(#d9645e, 2026-07-18 채도 낮춤 — 원래 #ff3b30이었는데
  "전체 톤과 안 어울린다"는 요청으로 낮춤, 라이트/다크 공통). 표시 위치:
  달력 탭 날짜 칸(숫자 옆 작게), 오늘 탭(td-holiday-name), 날짜 상세
  ("(요일)" 옆), 달력·스케줄 위젯(날짜 숫자에 이어붙임, SpannableString),
  오늘 위젯(날짜 라벨 문자열에 그냥 이어붙임 — 네이티브 안 건드림). 새로
  날짜를 보여주는 화면·위젯을 만들 때 이 다섯 곳과 같은 방식으로 맞출 것.
- 테마: data-theme 명시(light/dark) > @media 시스템 감지.
  @media 규칙은 :root:not([data-theme])로 한정.
- **카테고리 기능은 UI에서 지워짐(2026-07-16, 사용자 요청)** — "할 일에
  반복·카테고리·중요 세 가지가 있는데 정리하고 싶다"는 요청으로, 카테고리
  기능을 없애고 반복·중요 두 가지로 단순화함. **데이터 계층은 그대로
  남겨둠**(나중에 필요하면 다시 UI만 붙이면 되게) — state.categories,
  CATEGORIES_DEFAULT, CAT_FALLBACK, CATEGORY_COLOR_PRESETS, getCategories()/
  getCat(id), migrateState()의 "기타" 고정 로직 전부 코드에 그대로 있고
  정상 동작함. **지운 건 사용자가 카테고리를 보거나 고르는 화면뿐**:
  - 설정 탭 "카테고리" 메뉴 항목·화면(renderCategorySettings, sd-categories,
    category-settings-card, btn-category-add) 전부 삭제.
  - "반복 할 일 만들기/전환/편집" 시트의 카테고리 고르기 버튼·팝업
    (promote-cat-picker/-btn/-menu, renderPromoteCatButton/-Menu) 전부 삭제 —
    이제 새로 만드는 반복 할일은 항상 category:'etc'로 저장됨(promoteCategoryId
    는 여전히 let로 남아있고, 이미 카테고리가 있던 예전 반복 할일을 열면
    openRepeatEditor가 그 값을 그대로 읽어와서 저장 시 안 바뀌게 함 — 고를
    UI가 없다고 기존 데이터를 지우진 않음).
  - splitCategoryInput() 함수도 카테고리 설정 화면 전용이라 같이 삭제.
  - 할 일 줄(buildTodoRow)·반복 할일 카드(buildRoutineCard)의 카테고리
    아이콘 배지(.item-icon/.routine-icon)도 완전히 삭제 — 이제 아이콘 자리
    자체가 없음(카테고리 있든 없든).
  - **다시 만들지 말 것**: 위 화면들을 되살리려 하지 말고, 필요해지면 먼저
    사용자에게 확인할 것 — 이번엔 명시적으로 "필요하면 나중에 다시 만들게"
    라며 지워달라고 한 것이라, 데이터가 남아있다고 자동으로 UI를 복원하면 안 됨.
  - 근무유형 색(SHIFT_COLORS/SHIFT_COLOR_PRESETS)이나 설정 화면 다른 메뉴는
    이 변경과 무관, 그대로 있음.
  - 위젯 4(오늘 전체 관리)의 payload(buildTodayWidgetPayload)는 여전히
    getCat(t.item.category)로 icon 필드를 채워 넘김 — 이건 건드리지 않음(예전
    카테고리 데이터가 남아있는 항목이면 위젯에서도 그 아이콘이 계속 보일 수
    있음, 네이티브 쪽 코드 변경이 필요 없는 낮은 위험이라 그대로 둠).
- **할 일 텍스트 색 = 반복/중요 여부로 결정(2026-07-16, 카테고리 색 대체)**:
  buildTodoRow/buildRoutineCard 둘 다 이 우선순위를 씀 — **중요 표시가
  있으면 주황(#ff9500, "확 눈에 튀게" 요청) > 아니면서 반복 할일이면 차분한
  초록(REPEAT_TEXT_COLOR = #5fa475, "너무 튀지 않게" 요청) > 둘 다
  아니면 평소 글씨색(var(--text-1), 라이트/다크 자동)**. buildTodoRow는
  li에 CSS 변수 `--todo-text-color`를 인라인으로 지정하고
  `.today-items .item-text { color: var(--todo-text-color, var(--text-1)); }`
  로 적용(예전엔 `--cat-color`였고 `#tab-today .item-text`에 별도로
  `var(--text-1)` 고정 오버라이드가 있어서 오늘 탭만 카테고리 색이 항상
  무시되는 불일치가 있었는데, 이번에 그 오버라이드도 지워서 오늘 탭·날짜
  상세 둘 다 항상 같은 규칙을 따르게 통일됨). buildRoutineCard는 전부
  반복 할일 목록이라 인라인 스타일로 직접 지정(기본 초록, important면 주황).
  **초록색은 처음엔 var(--accent)(앱 버튼 등에 쓰는 쨍한 초록 #34c759)를
  재사용했다가, "너무 눈에 띈다"는 요청으로 채도를 낮춘 전용 상수
  REPEAT_TEXT_COLOR = '#5fa475'로 교체함(2026-07-16, 같은 날 재조정)** —
  buildTodoRow/appendEventIndicators(.cell-event-item)/buildRoutineCard
  세 군데 모두 이 상수를 씀. 다시 var(--accent)를 재사용하지 말 것(버튼 등
  다른 UI 요소의 강조색과 헷갈리지 않도록 반복 할일 전용 색을 따로 둔 것).
  **처음엔 오늘 탭·날짜 상세·반복 할일 탭에만 적용했다가, 사용자가 "모든
  페이지·모든 위젯에서 말한 것"이라고 재확인해서 확장함(2026-07-16, 같은 날
  재수정)** — 놓쳤던 곳:
  - **달력 탭 날짜 칸(appendEventIndicators, .cell-event-item)**: 예전엔
    CSS에 `color: #ff9500`가 고정으로 박혀 있어서 칸 안의 할 일 글자가 항상
    무조건 주황이었음(반복/중요 여부와 무관) — 이게 사용자가 "달력에서 아직도
    주황색"이라고 신고한 원인. 각 항목(ev)에 `ev.repeat`(있으면 반복)·
    `ev.important`를 인라인 스타일로 확인해서 같은 우선순위(중요>반복>기본)로
    색을 매김. 이 함수는 달력 탭뿐 아니라 팀 근무 확인·복수선택 화면과도
    공유되므로 그 화면들도 같이 고쳐짐(별도 수정 불필요). CSS 기본값(`.cell-
    event-item`의 `color`)도 고정 주황에서 `var(--text-1)`로 바꿔둠(인라인이
    항상 덮어쓰지만, 기본값 자체가 틀린 색이면 안 되므로).
  - **연간 탭 월 상자의 "★ 중요 표시" 목록(.year-month-item)**: 이 목록은
    getImportantEventsForMonth()로 걸러서 항상 "중요 표시된 것"만 모은
    목록이라, 반복 여부 확인 없이 항상 주황으로 고정(우선순위상 중요가 항상
    이김).
  - **기간(멀티데이) 할일의 달력 띠(.cell-period-band)는 그대로 둠** — 배경색이
    있는 알약 모양 칩(보라 배경 + 흰 글자)이라 "평소 글씨색 위에 얹는 텍스트
    색" 개념과 다른 컴포넌트로 보고 이번 범위에서 제외함(사용자가 이것도
    바꿔달라고 하면 그때 다시 검토).
  - **네이티브 위젯(오늘/스케줄)은 이번에 안 건드림**: 이미 고정 주황이
    아니라 테마 중립 회색(widget_text_secondary)을 쓰고 있어서 "여전히
    주황" 버그는 없었음 — 다만 반복=초록/중요=주황 구분은 위젯에는 아직
    없음(JS payload에 그 구분을 안 실어 보내고, 네이티브 RemoteViews 쪽에도
    조건부 색칠 코드가 없음). 이건 웹 쪽 CSS 수정과 달리 JS payload 필드
    추가 + Java 쪽 setTextColor 분기 추가가 필요한 더 큰 작업이라 이번엔
    범위에서 뺌 — 사용자가 위젯도 색 구분을 원하면 별도로 진행할 것.
- 아이콘 체계: 앱 고정 아이콘(하단 탭바, 설정 메뉴, "다른 팀 근무 확인"/
  "미배치"/"달력보기" 버튼)은 전부 선 아이콘 SVG로 통일(viewBox 0 0 24 24,
  stroke:currentColor, fill:none, stroke-width:1.8, round cap/join — 하단
  탭바 스타일 그대로). 설정 메뉴용은 SETTINGS_ICONS 상수에 모아둠(카테고리
  메뉴가 지워지면서 tag 항목도 같이 지움).
- 달력 칸 렌더링은 화면 간 공유(달력 탭/팀 근무/복수선택 통일 유지).
- **날짜 칸 줄 높이 통일 + 할 일 3줄**: 근무 이름을 보여주는 줄이 두 가지
  모양(하루짜리는 .cell-shift-badge, 여러 날 이어지는 근무는
  .cell-shift-band)으로 나뉘는데, 둘 다 높이 16px로 통일돼 있음(badge 쪽에
  높이가 고정 안 돼있으면 band와 실제 줄 높이가 달라 칸마다 들쭉날쭉해
  보임). 오늘 칸 강조는 `border`가 아니라 `outline`(+ 음수 offset) — border는
  칸 높이를 그만큼 키워서 오늘이 든 주(week)만 다른 주보다 미묘하게 커
  보이는 원인이었음, outline은 레이아웃 크기에 영향을 안 줘서 이 문제가
  없음. 하루짜리 할 일은 각 칸에 3줄까지 보임(appendEventIndicators,
  .slice(0,3)) — 다른 여백(cell-date 아래 여백, cell-event-list 위 여백·줄
  간격, 칸 자체 위아래 패딩)을 좁혀서 전체적으로 너무 안 커지게 함.

## 화면 구성 (하단 탭 5개)
- 연간 / 달력(구 전체보기) / 오늘 / 반복·중요(구 루틴, 구 "반복 할일" —
  2026-07-17에 중요 표시 항목도 같이 보여주는 화면으로 넓어지며 이름도
  바뀜, 코드상 id/switchTab 키는 여전히 'routine') / 설정 (+ 미배치 버튼은
  전 화면 공통) — "오늘"이 하단 탭 5개 중 정가운데(2026-07-16, 원래는 반복
  할일과 순서가 반대였는데 "오늘 위치를 가운데로" 요청으로 둘을 맞바꿈).
  switchTab()은 탭 이름으로만 화면·버튼을 찾아 전환하므로(document.
  getElementById('nav-'+name)/('tab-'+name)) 버튼을 나열하는 순서를 다시
  바꿔도 코드 쪽은 안 건드려도 됨 — nav 버튼들의 HTML 순서만 바뀌면 됨.
- **설정 탭 메뉴 묶음(2026-07-18 추가)**: 항목 7개가 구분 없이 한 줄로
  나열돼 있어서 "성격별로 묶어달라"는 요청으로 작은 소제목(.section-label,
  다른 화면 섹션 라벨과 같은 스타일 재사용)만 얹음 — 근무(교대 주기·근무
  편집·근무유형별 알람) / 화면(주 시작 요일·테마) / 기타(팀 근무·데이터
  백업). renderSettings()의 GROUPS 배열이 라벨과 순서를 정하고, 실제 항목
  정의(ITEMS)는 그대로 둔 채 GROUPS의 keys로 끌어와 순서대로 그림 — 새
  설정 항목을 추가하면 ITEMS뿐 아니라 GROUPS 중 한 곳의 keys에도 추가해야
  화면에 보임(안 넣으면 조용히 안 보임, filter(Boolean)로 걸러짐). 접었다
  펼치는 방식은 항목이 7개뿐이라 과하다고 판단해 안 씀 — 늘어나면 그때
  재검토.
- 연간 탭: 1~3/4~6/7~9/10~12월을 4줄×3칸으로, 각 달 상자를 누르면 그 달의
  달력 탭으로 이동. 달 상자엔 그 달에 "★ 중요 표시"된 할 일(반복이든
  한 번짜리든 상관없음, getImportantEventsForMonth)을 그 달에서 처음
  나타나는 날짜 순으로 최대 3개까지 보여줌. 생일처럼 매년 반복되는 것도
  중요 표시만 해두면 매년 뜸 — 반복이라고 걸러내지 않고 사용자가 별표로
  알아서 정하게 함.
- ★ 중요 표시는 buildTodoRow()에서 반복 할일·한 번짜리 할 일 모두에 똑같이
  보임(item.important). 반복으로 전환할 때도 그대로 이어받는다.
- 날짜 상세는 "반복 할일 + 한 번짜리 할 일"을 한 목록으로 합쳐서 보여줌
  (따로 섹션 나누지 않음, renderTodoList). **맨 앞 숫자로 시각이 인식되면
  그 시각 순서대로 정렬됨(2026-07-17 추가)** — 오늘 탭 타임라인에서 쓰는
  parseLeadingTimeFromText를 그대로 재사용하되, 여기는 구역 구분이 없는
  flat 목록이라 저장된 timeSlot/timelineOrder를 쓰지 않고 렌더링하는 순간에
  텍스트에서 바로 계산함(repeatItems.concat(oneTimeItems) 후 정렬) — **반복
  할일도 정렬 대상에 포함**(오늘 탭 타임라인 배치 때는 "타이핑 중엔 인식 안
  함" 원칙 때문에 반복 할일을 일부러 뺐지만, 그건 저장 시점 얘기고 여기는
  화면에 뿌릴 때 읽기만 하는 것이라 상관없이 적용됨 — 혼동하지 말 것). 시각이
  인식된 항목끼리는 이른 시각순으로 먼저, 인식 안 된 항목은 원래 순서(반복
  먼저 → 한 번짜리, 각자 배열 순서 그대로) 그대로 뒤에 이어붙임.
- 오늘 탭은 renderTodoList 대신 renderTodayTimeline으로 그림 — 미정(맨 위)·
  오전·오후(~18시)·밤 4구역으로 나눠서 보여줌(위 timeSlot 설명 참고).
  이 구분은 오늘 탭에만 적용, 날짜 상세는 안 바뀜.
- **맨 앞 숫자로 시각 자동 인식(2026-07-16 추가, parseLeadingTimeFromText/
  applyAutoTimeSlot)**: 할 일 글자 맨 앞에 숫자를 붙이면 그 시각으로 자동
  인식해서 오전/오후(~18시)/밤 구역에 자동 배치 + 구역 안에서 시간순 정렬됨.
  예: "1800청소"→18:00(밤), "18청소"→18:00(밤, 분 없으면 정시), "1630머리"
  →16:30(오후). **정확히 2자리 또는 4자리 숫자일 때만 인식**(뒤에 숫자가
  더 붙어 있으면 그 매칭 자체가 실패함) — 3자리("830청소") 등 다른
  자리수나, 범위를 벗어난 값(시 24 이상, 분 60 이상)은 시각으로 안 보고
  그냥 일반 텍스트로 취급(사용자가 직접 확인). **화면에 보이는 글자는 숫자를
  지우지 않고 입력한 그대로 둠**(사용자 요청) — 인식된 숫자는 배치(timeSlot)·
  정렬(timelineOrder = 자정부터의 분 수)에만 내부적으로 씀, 별도 시각
  라벨은 안 보여줌.
  - **인식 시점**: 타이핑하는 도중이 아니라, 할 일이 날짜를 얻는 순간
    (배치)이나 이미 배치된 할 일의 글자를 고칠 때(startInlineTextEdit
    commit, blur/Enter 시점 — 'input' 이벤트마다는 아님)에만 계산함
    (applyAutoTimeSlot 호출 지점: submitQuickAdd의 즉시 배치 분기,
    placeInboxItem, syncWidgetDatedItems, startInlineTextEdit) — 사용자가
    명시적으로 "타이핑할 때 인식 말고, 카드가 날짜에 들어왔을 때 인식"을
    요청함. 미배치 상태(날짜 없음)인 항목은 대상 아님(ev.date 없으면
    applyAutoTimeSlot이 바로 리턴).
  - **반복 할일은 대상 아님**(의도적 범위 제외) — 반복 할일 편집 화면
    (openRepeatEditor)은 글자를 타이핑할 때마다(promote-text-input의
    'input' 이벤트) syncPromoteEditorToState가 바로바로 저장하는 구조라,
    거기에 이 인식을 걸면 "타이핑 중엔 인식 안 함" 원칙과 정면으로 부딪힘
    (커밋 시점이 따로 없음) — 그래서 반복 할일의 시간대는 지금처럼 계속
    직접 드래그로만 정함. 나중에 반복 할일에도 적용하려면, 그 편집 화면에
    별도의 "포커스 아웃/저장 시점" 같은 걸 새로 만들어야 함(현재 구조로는
    타이핑마다 도는 걸 피할 방법이 없음).
  - **숫자가 있으면 항상 그 숫자가 우선**: 이미 손으로 드래그해서 다른
    구역으로 옮겨둔 할 일이라도, 나중에 글자를 다시 고쳐서 맨 앞 숫자가
    유지되거나 바뀌면 그 숫자 기준으로 구역이 다시 계산됨(단순한 규칙 유지가
    목적 — "숫자가 있으면 그게 곧 배치"). 자동 배치를 원치 않으면 맨 앞
    숫자를 빼고 쓰거나, 배치된 뒤 손으로 다시 드래그하면 됨(단, 그 뒤에
    글자를 또 고치면서 숫자가 그대로 남아 있으면 다시 숫자 기준으로 돌아감).

## 안 만들 것 (포지션 유지)
- 범용 습관 트래커·만능 투두·일반 근무자용 확장 금지.
  '교대근무자'라는 틈에 집중. 넓히려는 유혹을 참는다.
