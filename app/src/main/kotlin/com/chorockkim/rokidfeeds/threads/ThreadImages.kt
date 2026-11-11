package com.chorockkim.rokidfeeds.threads

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.chorockkim.rokidfeeds.data.BoundedHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Threads 사진 받기·줄이기·캐시. 띠 폭(480)의 두 배를 넘지 않게 줄여 메모리를 아낀다. 실패하면 null(화면에 "사진을 못 불러옴").
 */
object ThreadImages {
    private val http = BoundedHttp(maxBodyBytes = 6 * 1024 * 1024)
    private val cache = LruCache<String, ImageBitmap>(24)

    suspend fun load(url: String): ImageBitmap? {
        cache.get(url)?.let { return it }
        val bitmap = runCatching {
            val bytes = http.fetchBytes(url)
            withContext(Dispatchers.Default) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= MAX_WIDTH) sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?.asImageBitmap()
            }
        }.getOrNull() ?: return null
        cache.put(url, bitmap)
        return bitmap
    }

    private const val MAX_WIDTH = 960

    /**
     * 녹색 단색 화면용: 밝기만 남겨 강조색(#00ff66)으로 그린다(rokid-conversate와 같은 방식, 사진은 흑백 밝기 → 녹색 채널).
     * R = 0, G = 밝기, B = 밝기 × 0.4.
     */
    val GreenFilter: ColorFilter = ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0f, 0f, 0f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.12f, 0.235f, 0.046f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}

/** 사진 하나의 상태: 받는 중(null, false), 실패(null, true), 성공(이미지). */
data class ImageState(val image: ImageBitmap?, val failed: Boolean)

@Composable
fun rememberThreadImage(url: String): ImageState {
    var state by remember(url) { mutableStateOf(ImageState(null, false)) }
    LaunchedEffect(url) {
        val image = ThreadImages.load(url)
        state = ImageState(image, failed = image == null)
    }
    return state
}
