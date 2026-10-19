# news-feed

한국어 뉴스 RSS(연합뉴스·동아일보·경향신문)를 **하나의 통합 목록**으로 합쳐 목록→상세로 보여 주는 Android 앱. Rokid 글래스용 단색 HUD UI를 폰 에뮬레이터에서 개발한 **시범 프로젝트**다.
공통 규칙은 `~/orca/projects/CLAUDE.md`를 따른다.

## 이 프로젝트에서 다른 점

- Kotlin + Jetpack Compose(`foundation`만, Material3 없음). 의존성은 최소로 유지하고 추가할 때 이유를 적는다.
- 빌드: `./gradlew assembleDebug`, 테스트: `./gradlew testDebugUnitTest`. JDK 17, Gradle 9.6.0, AGP 9.4.1.
- compileSdk는 36이다(설치된 SDK 최대). 최신 Compose BOM·core는 37을 요구하므로 `gradle/libs.versions.toml`의 버전을 올릴 때는 SDK 37을 먼저 설치해야 한다.
- UI는 글래스 규격을 따른다: 480×352 고정 캔버스, `#40ff5e` 단색과 투명도(100/60/40/8%), 검정 배경, 다른 색 금지(오류도 녹색), 그림자 대신 윤곽선. 토큰은 `ui/Theme.kt`의 `Glass`에 둔다.
- 입력은 **←/→, Enter, Back만** 쓴다(공통 컨벤션의 "Rokid 글래스 UI 입력 가정"). 목록은 ←/→로 양방향 순환하고 상세는 ←/→로 쪽을 넘기며(끝에서 멈춤) ↑↓는 처리하지 않는다. 키 → `NavKey` 매핑은 `ui/Keys.kt` 한 곳에 두고, 내비게이션 규칙은 `nav/Navigator.kt`(Android 없는 순수 상태머신)에만 둔다. 규칙을 바꾸면 `NavigatorTest`를 먼저 고친다.
- 상세는 스크롤 없이 쪽 단위다. 쪽당 글자 수는 `util/Pager.kt`의 상수(첫 쪽 220, 나머지 320, 링크 70)이며, 상수를 바꾸면 에뮬레이터에서 가장 긴 제목·본문으로 넘침을 눈으로 확인한다.
- 네트워크는 `data/BoundedHttp.kt`를 통해서만 한다(본문 4MiB, 리다이렉트 4회, 타임아웃 8초, 스킴·다운그레이드 검사). 설계 근거는 `rokid/docs/05-nexus-feature-control-patterns.md`.
- 뉴스 출처는 `data/NewsApi.kt`의 `SOURCES`에만 두고, 언론사별 10개씩 받아 `data/NewsMerge.kt`에서 최신순으로 합친다. 일부 언론사만 실패하면 성공한 기사만 보여 주고 실패한 이름을 푸터에 표시한다. 피드를 추가하기 전에 HTTP 200, UTF-8, 요약(description) 유무, 크기를 직접 확인한다(한겨레는 요약 없음, SBS는 790KB라 제외했다).
- RSS는 정규식으로 `<item>`만 읽는다(`data/RssParser.kt`). DTD·엔티티 해석을 하지 않으려는 선택이며, 이 한계는 파서 주석에 적혀 있다.
- 순수 로직(`RssParser`, `mergeArticles`, `htmlToText`, `timeAgo`)과 `NewsApi`(로컬 HTTP 서버)는 JVM 단위 테스트로 검증한다.
- 기사 본문과 요약의 저작권은 각 언론사에 있다. 이 앱은 RSS가 제공하는 제목·요약·링크만 표시하고 원문 링크를 함께 보여 준다.
- 실기기(Rokid 글래스) 설치는 허락을 받은 뒤에 한다. 현재 연결된 기기는 없다.
- 시작할 때 인터넷이 없으면 Wi-Fi 연결 화면이 먼저 뜬다(`ui/WifiScreen.kt`). Enter(탭)로 `local.properties`의 `wifi.ssid`/`wifi.password`에 `WifiNetworkSpecifier`로 연결한다(`net/WifiConnector.kt`). 값은 `local.properties.example`을 복사해 채우고 커밋하지 않는다.
- Wi-Fi 비밀번호는 빌드 시 APK(`BuildConfig`)에 들어간다. APK를 공유·배포할 때는 개인 Wi-Fi를 넣지 않는다. 비밀번호는 화면과 로그에 출력하지 않는다.
- 시스템이 첫 연결 때 확인 창("Connect to device")을 띄운다. 앱이 건너뛸 수 없으므로 "완전 자동 연결"이라고 쓰지 않는다.
