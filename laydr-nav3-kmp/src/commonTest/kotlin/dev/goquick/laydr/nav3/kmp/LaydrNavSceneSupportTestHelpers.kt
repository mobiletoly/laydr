package dev.goquick.laydr.nav3.kmp

import dev.goquick.laydr.core.LaydrRoute

internal fun testSceneSupport(
    list: LaydrRoute,
    detail: LaydrRoute,
): LaydrNavSceneSupport =
    laydrNavSceneSupport(
        listDetailScenes = listOf(
            LaydrNavListDetailSceneSupport(
                listRoute = list,
                detailRoute = detail,
                listMetadata = mapOf("test:list" to list.id),
                detailMetadata = mapOf("test:detail" to detail.id),
            ),
        ),
    )
