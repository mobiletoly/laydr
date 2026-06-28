package dev.goquick.laydr.examples.nav3kmpshopping.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.goquick.laydr.examples.nav3kmpshopping.Nav3KmpShoppingApp
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        Nav3KmpShoppingApp()
    }
}
