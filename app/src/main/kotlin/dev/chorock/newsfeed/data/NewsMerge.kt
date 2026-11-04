package dev.chorock.newsfeed.data

/**
 * 소스별 글을 하나로 합친다. **소스를 번갈아 한 개씩** 끼워 넣어(라운드 로빈) 시각이 늦은 소스(GeekNews 등)도 앞쪽에 보이게 한다.
 * 전체를 시각순으로만 정렬하면 몇 분 전 속보가 앞을 채워 몇 시간 전 글은 뒤로 밀리기 때문이다.
 * 각 소스 안에서는 최신순이고(시각을 모르는 글은 그 소스의 맨 뒤), 같은 차례에서는 입력 순서(소스 순서)를 유지한다.
 */
fun mergeArticles(perSource: List<List<Article>>): List<Article> {
    val sorted = perSource.map { articles -> articles.sortedByDescending { it.publishedAt } }
    val rounds = sorted.maxOfOrNull { it.size } ?: 0
    return buildList {
        for (round in 0 until rounds) {
            for (articles in sorted) articles.getOrNull(round)?.let(::add)
        }
    }
}
