package dev.goquick.laydr.examples.nav3kmpshopping.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.goquick.laydr.examples.nav3kmpshopping.Nav3KmpShoppingApp

fun main(): Unit = application {
    Window(onCloseRequest = ::exitApplication, title = "Laydr Nav3 KMP Shopping") {
        Nav3KmpShoppingApp()
    }
}
