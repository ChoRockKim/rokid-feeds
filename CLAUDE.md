# rokid-feeds

한국어 뉴스 RSS(연합뉴스·동아일보·경향신문)와 GeekNews(Atom)를 **하나의 통합 목록**으로 합쳐 목록→상세로 보여 주는 Rokid 안경 앱.
화면·입력·네트워크·배포는 다른 Rokid 앱(rokid-lens, rokid-youtube, rokid-conversate, rokid-glass-ios-navigation)과 같은 규칙을 따른다.
공통 사실·방법은 `ChoRockKim/rokid-conversate`의 `docs/rokid-dev-notes.md`에 있다.

## 먼저 읽을 것
- `README.md` — 화면(스냅샷), 조작, 설치·업데이트, 확인 상태

## 작업 규칙
- 사실을 쓸 때 **실기**(안경에서 확인) / **서버** / **에뮬레이터·빌드** / **문서**(소스·문서만 읽음) / **미확인**을 구분한다. 클라우드 세션은 안경을 못 쓰므로 "실기 미확인"을 적는다.
- 토큰·키(릴리스 키스토어, `OTA_UPLOAD_TOKEN`)는 환경 변수로만 쓴다. 저장소·로그·문서에 넣지 않는다.
- 빌드: `./gradlew :app:assembleDebug`, 테스트: `./gradlew :app:testDebugUnitTest`. JDK 17+, Gradle 9.6.0, AGP 9.4.1, compileSdk 36.
- 테스트 이름이 한국어라 `LANG=C.UTF-8`이 아닌 환경에서는 Kotlin 컴파일이 경로 오류로 실패한다.
- 클라우드에서 Maven Central이 429를 주면 Google 미러(`maven-central.storage-download.googleapis.com/maven2`)로 바꾸는 세션 전용 init 스크립트(`~/.gradle/init.d/`)를 쓴다. 저장소 설정은 그대로 둔다.
- 화면을 바꾸면 `./gradlew :app:recordPaparazziDebug`로 스냅샷(`app/src/test/snapshots/images`)을 다시 만들어 함께 커밋한다.
- 실기 설치는 사용자가 한다(adb 케이블). 첫 설치 뒤로는 OTA로 올린다.

## 화면 (다른 Rokid 앱과 같음)
- 480×282 논리 단위 띠를 화면 아래 가운데에 그린다. `MainActivity.attachBaseContext`가 화면 가로가 480dp가 되도록 밀도를 맞추고(글꼴 배율 무시), `ui/GlassScreen.kt`가 띠를 그린다. 띠 위는 검정(안경에서 투명).
- 여백 18, 강조 `#00ff66`, 보조 `#4da86c`, 윤곽선 투명도 40%. 토큰은 `ui/Theme.kt`의 `Glass`에만 둔다. 밝은 면·다른 색(오류 포함)은 쓰지 않는다. 이모지는 컬러로 그려지므로 쓰지 않는다.
- 상태(업데이트 · Wi-Fi/인터넷 · 지난번 튕긴 이유 · 시각/배터리)는 띠 바로 위 작은 알약으로만 보여 준다. 시스템 화면(Wi-Fi 설정 등)은 밝고 커서 앞을 가리므로 앱이 저절로 열지 않는다.

## 입력 (다른 Rokid 앱과 같음)
- 터치패드 키 → `input/GlassesInput.kt`(rokid-lens 그대로): 쓸기 = DPAD 좌우·183/184, 탭 = ENTER·202, 세로 쓸기 = DPAD 위/아래, 링 미디어 키, 방향 키 240ms 디바운스·`repeatCount > 0` 무시. Back은 시스템 Back으로.
- R08 링 → `input/RingTarget.kt`: 띠 위 투명 View 하나만 접근성 노드로 노출하고(Compose 쪽은 `NO_HIDE_DESCENDANTS`) SCROLL_FORWARD/BACKWARD/CLICK을 받는다. 행마다 포커스를 두지 않는다.
- 동작 → `NavKey` 매핑은 `input/NavKeys.kt` 한 곳, 규칙은 `nav/Navigator.kt`(Android 없는 순수 상태머신)에만 둔다. 규칙을 바꾸면 `NavigatorTest`를 먼저 고친다. 화면(`FeedScreen`, `DetailScreen`)은 상태를 보여 주기만 한다.
- 목록은 이전/다음이 양끝에서 순환, 상세는 쪽 이동이며 끝에서 멈춘다. 탭 = 열기/다음 쪽, 뒤로 = 목록/종료.

## 네트워크
- Wi-Fi 고르기·연결은 **시스템에 맡긴다**(저장된 iPhone 핫스팟에 자동 연결). 앱이 네트워크를 고르면(`WifiNetworkSpecifier`) 시스템 확인 창이 뜨므로 쓰지 않는다.
- Wi-Fi가 꺼져 있으면 `net/WifiEnabler.kt`(rokid-lens 그대로)가 rokid-conversate의 **"Wi-Fi 켜기 도우미"**(`com.chorockkim.conversate.wifihelper`, targetSdk 28) 등으로 켠다. 본앱은 무확인 OTA 때문에 targetSdk를 28로 낮출 수 없다.
- 피드를 못 받았는데 오프라인이면 "인터넷 연결 없음"을 띄우고 연결이 돌아오면 저절로 다시 받는다(`ui/NewsApp.kt`).
- 뉴스·본문 요청은 `data/BoundedHttp.kt`만 쓴다(본문 4MiB, 리다이렉트 4회, 타임아웃 8초, 스킴·다운그레이드 검사). OTA APK 받기만 `update/Updater.kt`가 직접 스트리밍한다(빌드 시 고정한 우리 서버 하나).

## 업데이트 (다른 Rokid 앱과 같음)
- 패키지 `com.chorockkim.rokidfeeds`, minSdk 26, targetSdk 35, versionCode = X*10000+Y*100+Z(`-PappVersion`).
- 켤 때마다 rokid-conversate 서버(server-v2)의 `/apps/rokid-feeds/latest`를 확인하고, 새 버전이면 받아 SHA-256·패키지·버전·서명을 확인한 뒤 `USER_ACTION_NOT_REQUIRED`로 설치, `MY_PACKAGE_REPLACED`에서 다시 띄운다.
- 릴리스는 `scripts/release.sh X.Y.Z [--upload]`, **고정 키**(다른 Rokid 앱과 같은 키스토어·별칭 `rokidyoutube`). 키를 잃으면 OTA가 불가능하다.
- 서버의 `OTA_PUBLIC_APPS`에 `rokid-feeds`가 있어야 한다(rokid-conversate 쪽 설정).

## 뉴스
- 출처는 `data/NewsApi.kt`의 `SOURCES`에만 두고, 소스별 10개씩 받아 `data/NewsMerge.kt`에서 번갈아 한 개씩 끼워 합친다(시각순으로만 정렬하면 GeekNews 글이 뒤로 밀린다). 각 소스 안에서는 최신순. 일부만 실패하면 성공한 기사만 보여 주고 실패한 이름을 푸터에 표시한다. 피드를 추가하기 전에 HTTP 200, UTF-8, 요약 유무, 크기를 직접 확인한다(한겨레는 요약 없음, SBS는 790KB라 제외).
- RSS는 정규식으로 `<item>`만 읽는다(`data/RssParser.kt`, DTD·엔티티 해석 안 함).
- 상세는 스크롤 없이 쪽 단위다. 쪽당 글자 수는 `util/Pager.kt`의 상수(첫 쪽 200, 나머지 290, 링크 70). 바꾸면 스냅샷으로 가장 긴 제목·본문의 넘침을 확인한다.
- 상세를 열면 연합뉴스·동아일보·경향신문은 원문 페이지에서 본문을 최대 2,400자 받는다(`data/ArticleBody.kt`, 소스별 규칙). 못 뽑으면 RSS 요약으로 폴백. 저작권은 각 언론사에 있다.
