package dev.chorock.newsfeed.util

/** 초 단위 시각 두 개로 "3시간 전" 같은 짧은 문구를 만든다. 미래 시각은 "방금"으로 본다. */
fun timeAgo(nowSeconds: Long, thenSeconds: Long): String {
    val diff = nowSeconds - thenSeconds
    return when {
        diff < 60 -> "방금"
        diff < 3_600 -> "${diff / 60}분 전"
        diff < 86_400 -> "${diff / 3_600}시간 전"
        else -> "${diff / 86_400}일 전"
    }
}
