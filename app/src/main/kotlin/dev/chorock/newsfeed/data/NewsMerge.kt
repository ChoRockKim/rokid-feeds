package dev.chorock.newsfeed.data

/**
 * 언론사별 기사를 하나로 합쳐 최신순으로 정렬한다.
 * 발행 시각을 알 수 없는 기사(0)는 맨 뒤로 가고, 시각이 같으면 입력 순서(언론사 순서)를 유지한다.
 */
fun mergeArticles(perSource: List<List<Article>>): List<Article> =
    perSource.flatten().sortedByDescending { it.publishedAt }
