package com.chorockkim.rokidfeeds.ui

import androidx.compose.runtime.saveable.listSaver
import com.chorockkim.rokidfeeds.nav.NavState

/** [NavState]를 화면 회전 등에서 보존한다. 열린 기사가 없으면 -1로 저장한다. */
val NavStateSaver = listSaver<NavState, Int>(
    save = { listOf(it.selected, it.openIndex ?: -1, it.page) },
    restore = { NavState(selected = it[0], openIndex = it[1].takeIf { index -> index >= 0 }, page = it[2]) },
)

/** [com.chorockkim.rokidfeeds.nav.AppState]를 보존한다. */
val AppStateSaver = listSaver<com.chorockkim.rokidfeeds.nav.AppState, Int>(
    save = { listOf(it.screen.ordinal, it.menuSelected) },
    restore = { com.chorockkim.rokidfeeds.nav.AppState(com.chorockkim.rokidfeeds.nav.Screen.entries[it[0]], it[1]) },
)
