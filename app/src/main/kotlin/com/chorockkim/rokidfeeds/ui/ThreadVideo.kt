package com.chorockkim.rokidfeeds.ui

import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

/** 한 번의 앞뒤 옮기기 요청([id]가 바뀔 때마다 한 번). */
data class SeekRequest(val seconds: Int = 0, val id: Int = 0)

/** 재생 상태(화면 표시용). */
private sealed interface VideoUi {
    data object Loading : VideoUi
    data class Playing(val positionMs: Int, val durationMs: Int) : VideoUi
    data object Ended : VideoUi
    data class Failed(val what: Int) : VideoUi
}

/**
 * 동영상 재생(사진 화면에서 동영상을 탭하면). 안드로이드 MediaPlayer가 threads 동영상 주소(MP4, 로그인 없이 받힘)를
 * 바로 스트리밍하고 TextureView에 그린다. 사진과 같게 **녹색 단색**으로 보이도록 TextureView 그리기에 색 행렬을 건다
 * (밝은 색을 쓰지 않는 규칙). 소리는 안경 스피커로 나간다.
 * 탭 = 일시정지/계속([paused]), 쓸기 = [SeekRequest]만큼 앞뒤로, 뒤로 = 사진. 화면을 떠나거나 앱이 가려지면 멈추고 놓는다.
 * 실기 미확인(코덱·속도·소리 크기).
 */
@Composable
fun ThreadVideoScreen(
    author: String,
    url: String,
    paused: Boolean,
    seek: SeekRequest,
    index: Int,
    total: Int,
    /** 스냅샷에서는 플레이어를 만들지 않는다. */
    preview: Boolean = false,
) {
    var ui by remember(url) { mutableStateOf<VideoUi>(VideoUi.Loading) }
    var aspect by remember(url) { mutableStateOf(0.8f) }
    val player = remember(url) { if (preview) null else MediaPlayer() }
    var prepared by remember(url) { mutableStateOf(false) }

    if (player != null) {
        DisposableEffect(player) {
            player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build())
            player.setOnPreparedListener { mp ->
                prepared = true
                if (mp.videoWidth > 0 && mp.videoHeight > 0) aspect = mp.videoWidth.toFloat() / mp.videoHeight
                mp.start()
                ui = VideoUi.Playing(0, mp.duration)
            }
            player.setOnCompletionListener { ui = VideoUi.Ended }
            player.setOnErrorListener { _, what, _ -> ui = VideoUi.Failed(what); true }
            runCatching {
                player.setDataSource(url)
                player.prepareAsync()
            }.onFailure { ui = VideoUi.Failed(-1) }
            onDispose { runCatching { player.stop() }; player.release() }
        }
        // 탭으로 일시정지/계속(끝난 뒤 탭하면 처음부터).
        LaunchedEffect(player, paused, prepared) {
            if (!prepared) return@LaunchedEffect
            runCatching {
                if (paused) player.pause()
                else if (!player.isPlaying) { if (ui == VideoUi.Ended) player.seekTo(0); player.start() }
            }
        }
        LaunchedEffect(player, seek.id) {
            if (seek.id == 0 || !prepared) return@LaunchedEffect
            runCatching {
                val to = (player.currentPosition + seek.seconds * 1000).coerceIn(0, (player.duration - 500).coerceAtLeast(0))
                player.seekTo(to)
            }
        }
        // 진행 시간(1초마다).
        LaunchedEffect(player, prepared) {
            while (prepared) {
                runCatching { if (ui !is VideoUi.Failed && ui != VideoUi.Ended) ui = VideoUi.Playing(player.currentPosition, player.duration) }
                delay(1_000)
            }
        }
        // 앱이 가려지면 멈춘다.
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, player) {
            val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) runCatching { player.pause() } }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (player != null) {
                AndroidView(
                    factory = { context ->
                        TextureView(context).apply {
                            isFocusable = false
                            // 녹색 단색: 사진과 같은 색 행렬(빨강 0, 초록 = 밝기, 파랑 조금).
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, Paint().apply { colorFilter = ColorMatrixColorFilter(GREEN_MATRIX) })
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                    runCatching { player.setSurface(Surface(st)) }
                                }
                                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = Unit
                                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                    runCatching { player.setSurface(null) }
                                    return true
                                }
                                override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                            }
                        }
                    },
                    modifier = Modifier.aspectRatio(aspect, matchHeightConstraintsFirst = true).clip(RoundedCornerShape(12.dp)),
                )
            }
            when (val u = ui) {
                VideoUi.Loading -> BasicText("동영상 불러오는 중…", style = Glass.BodySmall)
                is VideoUi.Failed -> BasicText("동영상을 재생하지 못했습니다 (${u.what})", style = Glass.BodySmall)
                VideoUi.Ended -> BasicText("끝 · 탭하면 처음부터", style = Glass.BodySmall)
                is VideoUi.Playing -> if (paused) PlayBadge(44.dp)
            }
        }
        val time = (ui as? VideoUi.Playing)?.let { "${clock(it.positionMs)} / ${clock(it.durationMs)}" } ?: ""
        Row(Modifier.fillMaxWidth()) {
            BasicText(
                "@$author · 탭 " + (if (paused) "재생" else "일시정지") + " · ◀ ▶ ${com.chorockkim.rokidfeeds.nav.VIDEO_SEEK_S}초 · 뒤로 사진",
                style = Glass.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            BasicText(listOf(time, "${index + 1}/$total").filter { it.isNotEmpty() }.joinToString(" · "), style = Glass.Caption)
        }
    }
}

private fun clock(ms: Int): String {
    val s = (ms.coerceAtLeast(0) / 1000)
    return "%d:%02d".format(s / 60, s % 60)
}

/** ThreadImages.GreenFilter와 같은 행렬(android.graphics용). */
private val GREEN_MATRIX = floatArrayOf(
    0f, 0f, 0f, 0f, 0f,
    0.299f, 0.587f, 0.114f, 0f, 0f,
    0.12f, 0.235f, 0.046f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)
