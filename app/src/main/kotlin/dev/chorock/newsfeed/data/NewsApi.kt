package dev.chorock.newsfeed.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** 한국어 언론사 RSS를 가져와 하나의 피드로 합친다. */
class NewsApi(
    private val sources: List<FeedSource> = SOURCES,
    private val http: BoundedHttp = BoundedHttp(),
) {

    /**
     * 모든 언론사를 병렬로 받아 최신순으로 합친다. 일부만 실패하면 성공한 기사만 돌려주고 실패한 언론사 이름을 함께 알린다.
     * 전부 실패하면 예외를 던져 UI가 재시도 화면을 보여 준다.
     */
    suspend fun feed(perSource: Int = PER_SOURCE): Feed = coroutineScope {
        val results = sources.map { source ->
            async {
                runCatching { RssParser.parse(http.fetch(source.url), perSource).map { it.copy(source = source.name) } }
            }
        }.awaitAll()

        val failed = sources.filterIndexed { index, _ -> results[index].isFailure }
        if (failed.size == sources.size) {
            throw results.first().exceptionOrNull() ?: IllegalStateException("모든 언론사를 불러오지 못했습니다")
        }
        Feed(
            articles = mergeArticles(results.map { it.getOrDefault(emptyList()) }),
            failedSources = failed.map { it.name },
        )
    }

    companion object {
        /** Atom, 키 불필요. 2026-11-04에 HTTP 200·20건·한국어를 확인했다. */
        val GEEKNEWS = FeedSource("GeekNews", "https://news.hada.io/rss/news")

        /** 소스마다 최신 글을 이만큼씩 가져와 합친다(총 40개). */
        const val PER_SOURCE = 10

        /** 언론사 3곳은 2026-10-19에 모두 HTTP 200, UTF-8, 요약 포함을 확인했다. 한겨레(요약 없음)·SBS(790KB)는 제외. */
        val SOURCES = listOf(
            FeedSource("연합뉴스", "https://www.yna.co.kr/rss/news.xml"),
            FeedSource("동아일보", "https://rss.donga.com/total.xml"),
            FeedSource("경향신문", "https://www.khan.co.kr/rss/rssdata/total_news.xml"),
            GEEKNEWS,
        )
    }
}
