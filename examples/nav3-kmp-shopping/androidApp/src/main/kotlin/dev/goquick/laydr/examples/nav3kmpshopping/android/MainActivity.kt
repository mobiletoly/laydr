package dev.goquick.laydr.examples.nav3kmpshopping.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.goquick.laydr.examples.nav3kmpshopping.Nav3KmpShoppingApp

internal class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Nav3KmpShoppingApp()
        }
    }
}
