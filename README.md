# Rokid Feeds

Rokid 안경용 뉴스 앱입니다. 연합뉴스·동아일보·경향신문 RSS와 GeekNews(Atom)를 **하나의 통합 목록**(소스별 10개씩, 총 40개, 소스를 번갈아 끼워 넣음)으로 보여 주고, 기사를 열면 제목·언론사·기자·시간·본문·링크를 쪽 단위로 보여 줍니다.

화면 전체가 아래쪽 **480 × 282 띠** 안에 그려지고, 띠 위쪽은 검은색이라 안경에서는 투명하게 보입니다. rokid-lens, rokid-youtube, rokid-conversate, rokid-glass-ios-navigation과 같은 방식입니다(화면 크기·색·입력·Wi-Fi·OTA).

## 화면

| 통합 목록 | 상세 1쪽 | 상세 2쪽 (본문 받는 중) |
|---|---|---|
| ![feed](app/src/test/snapshots/images/com.chorockkim.rokidfeeds.ui_FeedSnapshotTest_feed.png) | ![detail](app/src/test/snapshots/images/com.chorockkim.rokidfeeds.ui_FeedSnapshotTest_detail_firstPage.png) | ![detail2](app/src/test/snapshots/images/com.chorockkim.rokidfeeds.ui_FeedSnapshotTest_detail_secondPage_loadingBody.png) |

| 인터넷 없음 (Wi-Fi 켜는 중) | 불러오기 실패 + 새 버전 알림 | 일부 언론사 실패 |
|---|---|---|
| ![offline](app/src/test/snapshots/images/com.chorockkim.rokidfeeds.ui_FeedSnapshotTest_offline.png) | ![error](app/src/test/snapshots/images/com.chorockkim.rokidfeeds.ui_FeedSnapshotTest_error_withUpdatePill.png) | ![partial](app/src/test/snapshots/images/com.chorockkim.rokidfeeds.ui_FeedSnapshotTest_feed_partialFailure.png) |

위 그림은 [Paparazzi](https://github.com/cashapp/paparazzi)가 실제 Compose 코드로 그린 480×640 화면입니다. 기사는 예시 문구입니다.

- 띠: 여백 18, 강조 `#00ff66`, 보조 `#4da86c`, 얇은 녹색 윤곽선. 밝은 면은 앞을 가리므로 만들지 않습니다.
- 띠 바로 위: 왼쪽 업데이트, 가운데 Wi-Fi·인터넷 상태(또는 지난번 앱이 튕긴 이유), 오른쪽 시각·배터리.

## 조작

| 동작 | 목록 | 상세 | 불러오는 중 / 오류 |
|---|---|---|---|
| 앞/뒤로 쓸기 · 링 앞/뒤 | 다음/이전 기사 (**양끝에서 순환**) | 다음/이전 쪽 (**끝에서 멈춤**) | — |
| 탭 · 링 탭 | 기사 열기 | 다음 쪽 | 다시 시도 |
| 뒤로 · 링 두 번 탭 | 앱 종료 | 목록으로 (선택 위치 유지) | 앱 종료 |

- **안경 터치패드:** rokid-lens·rokid-youtube와 같은 키 매핑(`input/GlassesInput.kt`, RokidPipe에서 실기 확인된 것). 쓸기는 DPAD 좌우·183/184, 탭은 ENTER·202, 세로 쓸기(DPAD 위/아래)도 이전/다음. 한 번 쓸기에 두 번 들어오는 이벤트는 240ms 디바운스로 거릅니다.
- **R08 링 ([R08 Access Bridge](https://github.com/Anezium/R08-Access-Bridge)):** 띠 위에 링 전용 노드(`input/RingTarget.kt`) 하나만 노출해서, 브리지가 보내는 스크롤 앞/뒤와 클릭을 위 동작으로 바꿉니다. 브리지가 꺼져 있을 때 들어오는 링 미디어 키도 같은 동작입니다.
- 내비게이션 규칙 전체는 Android 없는 순수 상태머신(`nav/Navigator.kt`)이며 "어떤 키 순서로도 뒤로를 두 번 이내로 누르면 종료에 닿음"을 테스트로 검증합니다.

## 인터넷과 Wi-Fi

- 네트워크 고르기는 **시스템에 맡깁니다.** iPhone 핫스팟을 한 번 연결해 두면 안경이 저절로 다시 붙습니다. 앱이 네트워크를 직접 고르면 시스템 확인 창이 떠서 쓰지 않습니다.
- 안경은 배터리 때문에 Wi-Fi를 꺼 두는 일이 많습니다. 꺼져 있으면 10초마다 rokid-conversate의 **"Wi-Fi 켜기 도우미"** 앱(targetSdk 28)으로 켭니다. 도우미가 없으면 띠 위에 `Wi-Fi 꺼짐 · 도우미 앱 없음`이 뜹니다. 시스템 Wi-Fi 화면은 열지 않습니다.
- 피드를 못 받았는데 인터넷이 없으면 "인터넷 연결 없음"을 보여 주고, 연결이 돌아오면 저절로 다시 불러옵니다.
- iPhone 핫스팟은 연결된 기기가 없으면 잠듭니다. 단축어 자동화(블루투스 → Rokid 안경 연결될 때 → 개인용 핫스팟 켜기)를 쓰면 손대지 않고 연결됩니다(rokid-conversate `docs/rokid-dev-notes.md` 1장).

## 기사 본문

RSS 요약은 언론사가 잘라 줍니다(연합뉴스 83자, 동아일보 약 500자, 경향신문 203자, 2026-11-04 확인). 그래서 상세를 열면 원문 링크에서 본문을 받습니다.

- 규칙(`data/ArticleBody.kt`, 정규식, 소스별): 연합뉴스는 `story-news` 영역의 속성 없는 `<p>`, 경향신문은 `articleBody` 아래 `content_text` 문단, 동아일보는 `news_view` 영역에서 스크립트·사진을 걷어 낸 텍스트. 최대 2,400자에서 문장 끝으로 자르고 `…`을 붙입니다.
- 받는 동안은 RSS 요약을 보여 주고 아래에 "본문 불러오는 중…"을 띄웁니다. 실패하거나 요약보다 짧으면 요약을 그대로 씁니다. GeekNews는 요약이 충분해 본문을 받지 않습니다.
- 쪽 나누기(`util/Pager.kt`): 첫 쪽 200자(제목·메타가 함께 보임), 나머지 290자, 링크는 70자로 환산. 문단 → 문장 끝 → 공백 순으로 자릅니다.

## 빌드와 설치

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

릴리스(고정 키로 서명, OTA 서버에 올리기):

```bash
RELEASE_KEYSTORE=... RELEASE_KEYSTORE_PASS=... OTA_UPLOAD_TOKEN=... scripts/release.sh 0.1.0 --upload
```

처음 한 번은 케이블로 설치합니다. 그다음부터는 앱을 켤 때 알아서 업데이트됩니다(확인 창 없음).

```bash
adb install -r -i com.chorockkim.rokidfeeds rokid-feeds-0.1.0.apk
adb shell appops set com.chorockkim.rokidfeeds REQUEST_INSTALL_PACKAGES allow
adb shell appops set com.chorockkim.rokidfeeds SYSTEM_ALERT_WINDOW allow
```

OTA 서버(rokid-conversate server-v2)의 `OTA_PUBLIC_APPS`에 `rokid-feeds`가 있어야 합니다. Wi-Fi 자동 켜기에는 rokid-conversate의 `wifihelper` APK가 한 번 설치돼 있어야 합니다.

미리보기: `adb shell am start -n com.chorockkim.rokidfeeds/.MainActivity --es feed geeknews`면 GeekNews만 보여 줍니다.

## 테스트

```bash
./gradlew :app:testDebugUnitTest      # 내비게이션, 입력 매핑, 쪽 나누기, RSS·병합·본문 추출, HTTP 상한, OTA 응답, Wi-Fi 문구
./gradlew :app:recordPaparazziDebug   # 화면 스냅샷 → app/src/test/snapshots/images
```

## 구조

```
app/src/main/kotlin/com/chorockkim/rokidfeeds/
├── MainActivity.kt        # 띠 크기 맞추기(밀도), 키·링 → NavKey, Wi-Fi·네트워크·시각·배터리, OTA
├── RokidFeedsApp.kt       # 튕긴 이유 기록(다음 실행 때 표시), Updater
├── data/                  # NewsApi(병렬 받기·부분 실패), BoundedHttp, NewsMerge, RssParser, ArticleBody, Models
├── input/                 # GlassesInput(터치패드 키), RingTarget(R08 링), NavKeys(→ NavKey 매핑 한 곳)
├── nav/Navigator.kt       # 순수 내비게이션 상태머신
├── net/WifiEnabler.kt     # Wi-Fi 켜 두기(도우미 앱), 상태 문구
├── ui/                    # GlassScreen(띠 + 상태 알약), NewsApp(상태), FeedScreen, DetailScreen, LoadState, Theme
├── update/                # Updater(확인·받기·검증·설치), Release, InstallResultReceiver
└── util/                  # Pager, HtmlText, TimeAgo
```

## 뉴스 출처

| 언론사 | RSS | 결과 | 요약 길이(중앙값) |
|---|---|---|---|
| GeekNews | `https://news.hada.io/rss/news` | 200, 20개, Atom(2026-11-04) | 약 200~500자 |
| 연합뉴스 | `https://www.yna.co.kr/rss/news.xml` | 200, 120개 | 83자 |
| 동아일보 | `https://rss.donga.com/total.xml` | 200, 50개 | 약 500자 |
| 경향신문 | `https://www.khan.co.kr/rss/rssdata/total_news.xml` | 200, 50개 | 약 200자 |

사용하지 않은 피드: 한겨레는 요약이 비어 있고, SBS는 790KB라 안경에는 무겁고, 매일경제는 403(Cloudflare), KBS·YTN은 404, JTBC는 항목이 1개뿐이었습니다(2026-10-19). 기사 저작권은 각 언론사에 있습니다.

## 확인한 것과 안 한 것

- **확인함 (클라우드 빌드, 2026-11-11):** `:app:testDebugUnitTest` 103개 통과, `:app:assembleDebug` 성공, 화면 스냅샷 7장.
- **이전 버전에서 에뮬레이터로 확인 (2026-10-19 ~ 11-04):** 실제 피드 4곳 로딩·병합, 본문 추출로 쪽 수 증가(연합뉴스 1→4쪽 등), 목록 순환·상세 쪽 멈춤·뒤로 두 번 종료. 이번 재구성 뒤 에뮬레이터로 다시 보지는 않았습니다.
- **안경에서 확인해야 함:** 터치패드·R08 링 조작, 띠 크기(밀도 조정), Wi-Fi 도우미로 켜기, OTA 무확인 설치, 실제 글꼴에서 쪽 넘침, 배터리.
- 다른 Rokid 앱의 실기 사실(키 매핑, Wi-Fi 도우미, 무확인 OTA)은 그 앱들에서 확인된 것이며 이 앱에서는 아직 실기 미확인입니다.
- 언론사 HTML 구조가 바뀌면 본문 추출이 실패해 RSS 요약으로 돌아갑니다. 피드 주소도 언론사가 바꿀 수 있습니다.
