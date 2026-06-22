package dev.goquick.laydr.examples.basic.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.goquick.laydr.examples.basic.ExampleApp

fun main(): Unit = application {
    Window(onCloseRequest = ::exitApplication, title = "Laydr Compose Basic") {
        ExampleApp()
    }
}
