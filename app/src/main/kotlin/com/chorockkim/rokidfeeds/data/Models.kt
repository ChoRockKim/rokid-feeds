package com.chorockkim.rokidfeeds.data

/** 한 언론사의 RSS 주소. */
data class FeedSource(val name: String, val url: String)

data class Article(
    val title: String,
    val link: String,
    /** RSS의 description을 평문으로 바꾼 요약. 없으면 빈 문자열. */
    val summary: String,
    val author: String?,
    /** 발행 시각(epoch 초). 알 수 없으면 0. */
    val publishedAt: Long,
    /** 언론사 이름. 파서는 비워 두고 [NewsApi]가 채운다. */
    val source: String = "",
)

/** 통합 피드. [failedSources]는 이번에 불러오지 못한 언론사 이름이다. */
data class Feed(val articles: List<Article>, val failedSources: List<String> = emptyList())
