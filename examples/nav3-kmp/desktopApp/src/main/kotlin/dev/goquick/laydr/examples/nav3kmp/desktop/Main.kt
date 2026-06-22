package dev.goquick.laydr.examples.nav3kmp.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.goquick.laydr.examples.nav3kmp.Nav3KmpAddressBookApp

fun main(): Unit = application {
    Window(onCloseRequest = ::exitApplication, title = "Laydr Nav3 Address Book") {
        Nav3KmpAddressBookApp()
    }
}
