package com.chorockkim.rokidfeeds.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.chorockkim.rokidfeeds.R
import org.junit.Rule
import org.junit.Test

/** 앱 아이콘 미리보기: 원형·둥근 사각 마스크(런처 모양)와 확대. */
class IconSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = 480, screenHeight = 200, xdpi = 160, ydpi = 160, density = Density.MEDIUM, orientation = com.android.resources.ScreenOrientation.LANDSCAPE, softButtons = false),
        theme = "android:Theme.Material.NoActionBar",
        showSystemUi = false,
    )

    @Test
    fun icon() = paparazzi.snapshot {
        Row(Modifier.background(Color(0xFF111111)).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            listOf(CircleShape, RoundedCornerShape(28.dp)).forEach { shape ->
                Box(Modifier.size(128.dp).clip(shape).background(Color.Black)) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(128.dp))
                }
            }
            Box(Modifier.size(64.dp).clip(CircleShape).background(Color.Black)) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(64.dp))
            }
        }
    }
}
