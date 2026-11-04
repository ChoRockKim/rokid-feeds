# news-feed

한국어 뉴스 RSS(연합뉴스·동아일보·경향신문)와 GeekNews(Atom, 개발 커뮤니티 큐레이션)를 **하나의 통합 목록**으로 합쳐 목록→상세로 보여 주는 Android 앱. Rokid 글래스용 단색 HUD UI를 폰 에뮬레이터에서 개발한 **시범 프로젝트**다.
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
- **에뮬레이터는 공유 자원일 수 있다.** 같은 adb 서버에 다른 세션이 붙어 있을 수 있으니, 이 프로젝트의 UI 확인은 전용 AVD(`NewsFeed_Test`, `~/.android/avd/`)를 `-port 5556`으로 띄우고 **모든 adb 명령에 `-s emulator-5556`을 붙인다**. `adb devices`에 모르는 기기가 있거나, 포그라운드에 이 앱이 아닌 앱이 있으면 키 입력·네트워크 변경을 멈추고 확인한다. 네트워크 설정(`svc data`, `cmd wifi`)을 바꿨으면 반드시 원래대로 되돌린다.
- Wi-Fi는 **이 앱의 HUD 화면**(홈 → 목록 → 폼)이며 OS 설정 화면이 아니다. 내비게이션은 `nav/WifiFlow`, 줄 구성과 입력 검증은 `nav/WifiItems`, 보안 판별은 `net/WifiSecurity`에 두고 순수 로직은 JVM 테스트로 고정한다. OS 설정 화면은 목록 맨 아래의 보험 줄로만 남겨 둔다.
- **Wi-Fi 목록 스캔에는 `ACCESS_FINE_LOCATION`(+위치 서비스)이 필요하다.** 안드로이드 13+에서도 `NEARBY_WIFI_DEVICES`로는 안 된다(공식 문서, API 36 에뮬레이터에서 확인). 스캔·연결 중 시스템 예외는 앱을 죽이지 않고 결과로 바꾼다.
- 사용자가 고른 Wi-Fi는 성공했을 때만 `net/WifiStore`(Keystore AES-GCM)로 저장한다. SSID·비밀번호를 로그·화면·평문 저장소에 내보내지 않는다. 저장값이 빌드 시 값(`local.properties`)보다 우선한다.
