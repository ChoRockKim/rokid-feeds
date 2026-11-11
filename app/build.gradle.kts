plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.paparazzi)
}

// 앱 버전 "X.Y.Z"는 -PappVersion으로 받는다(scripts/release.sh가 넘긴다). 다른 Rokid 앱과 같은 규칙:
// versionCode = X*10000 + Y*100 + Z라서 릴리스마다 이전 버전 위에 덮어 설치된다.
val appVersion = providers.gradleProperty("appVersion").orNull?.removePrefix("v") ?: "0.1.0"
val appVersionCode = appVersion.split(".").map { it.toInt() }.let { (x, y, z) -> x * 10000 + y * 100 + z }

// 릴리스 서명 키는 저장소 밖에 둔다(scripts/release.sh). 없으면 릴리스 빌드도 디버그 키로 서명한다(로컬용).
val releaseKeystore = providers.gradleProperty("RELEASE_KEYSTORE").orNull
val releaseKeystorePassword = providers.gradleProperty("RELEASE_KEYSTORE_PASS").orNull
val releaseKeyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").orNull ?: "rokidyoutube"
val relayUrl = providers.gradleProperty("relayUrl").orNull ?: "https://server-v2-production-8f97.up.railway.app"
// OTA: rokid-conversate 서버(server-v2)의 이 앱 폴더(update/Updater.kt).
val updateUrl = providers.gradleProperty("updateUrl").orNull
    ?: "https://server-v2-production-8f97.up.railway.app/apps/rokid-feeds"

android {
    namespace = "com.chorockkim.rokidfeeds"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chorockkim.rokidfeeds"
        // Rokid 글래스는 Android 12(API 31/32). 다른 Rokid 앱과 같은 값.
        minSdk = 26
        // 무확인 OTA(USER_ACTION_NOT_REQUIRED)는 targetSdk 29 이상이 필요하다. 그래서 Wi-Fi 켜기는 도우미 앱에 맡긴다.
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersion

        buildConfigField("String", "UPDATE_URL", "\"$updateUrl\"")
        // 폰 링크 로그인 중계(rokid-conversate 서버의 /login, /relay). Threads 로그인에 쓴다.
        buildConfigField("String", "RELAY_URL", "\"$relayUrl\"")
    }

    signingConfigs {
        if (releaseKeystore != null && releaseKeystorePassword != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeystorePassword // PKCS12: 저장소 비밀번호와 같다
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    // Threads 화면: 숨은 WebView에 데이터 전달 스크립트를 문서 시작 시 넣고, 로그인 화면은 어둡게 그린다.
    implementation(libs.androidx.webkit)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
