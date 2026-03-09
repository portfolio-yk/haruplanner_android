# 하루 플래너 (Haru Planner)
창업동아리 에이틱스 팀에서 제작한 학습 몰입 관리 솔루션 런처 앱

### 🚀 프로젝트 개요
단순한 일정 관리를 넘어, 사용자가 설정한 학습 시간 동안 스마트폰 사용을 물리적으로 제한하고 집중력을 극대화할 수 있도록 돕는 안드로이드 애플리케이션입니다.

### 🛠 기술 스택 (Tech Stack)
Language: Kotlin
UI Framework: Jetpack Compose
Architecture: Hilt (Dependency Injection)
Android System: Foreground Service, UsageStatsManager, AlarmManager, WindowManager
Hybrid Bridge: WebView JavaScript Interface
Data Storage: SharedPreferences

### 1️⃣ 실시간 앱 차단 시스템 (App Blocking)
사용자가 학습 중에 차단된 앱에 접근하는 것을 시스템 레벨에서 감시하고 즉시 차단합니다.
- Foreground Service 전환: 안드로이드의 백그라운드 실행 제한 정책을 해결하고 서비스의 가용성을 확보하기 위해 startForeground를 통해 포그라운드 상태를 유지합니다.
- 실시간 감시 루프: 코루틴 기반의 무한 루프를 통해 2초 간격으로 UsageStatsManager의 이벤트를 분석하며 현재 포그라운드 앱을 식별합니다.
- 강제 화면 점유: 차단 앱 실행 감지 시 WindowManager의 TYPE_APPLICATION_OVERLAY 속성을 가진 OverlayActivity를 최상단에 노출하여 사용자의 조작을 물리적으로 제한합니다.

### 2️⃣ 재부팅 및 화면 상태 기반 자동 런처 (Launcher Overlay)
기기가 꺼졌다 켜지거나 화면이 활성화될 때 잠금 화면을 강제로 띄워 학습 환경을 유지합니다.

- 부팅 및 화면 이벤트 수신: BroadcastReceiver를 통해 ACTION_SCREEN_ON 등 시스템 신호를 감지하여 잠금 서비스가 즉각 반응하도록 구현했습니다.
- START_STICKY 전략: 서비스의 onStartCommand에서 START_STICKY를 반환하여 시스템에 의해 서비스가 종료되거나 기기가 재부팅된 후에도 자동으로 재시작되도록 설계했습니다.
- 우회 방지 로직: 잠금 화면에서 뒤로가기 발생 시 Intent.CATEGORY_HOME을 호출하여 강제로 홈 화면으로 리다이렉션함으로써 차단 환경을 벗어날 수 없도록 처리했습니다.

### 기술적 도전 및 해결
백그라운드 제약 극복: 안드로이드 OS의 강화된 백그라운드 실행 제한 속에서 중단 없는 모니터링을 위해 포그라운드 서비스 및 상주 알림(Ongoing Notification) 전략을 도입했습니다.
최신 OS 버전 대응: Android 14(API 34)의 서비스 타입 선언 및 API 31 이상의 정확한 알람 권한(SCHEDULE_EXACT_ALARM) 정책을 준수하여 안정성을 확보했습니다.
하이브리드 아키텍처: 웹의 유연한 UI와 네이티브의 강력한 시스템 제어 기능을 WebviewInterface로 연동하여 효율적인 개발 구조를 구축했습니다.
