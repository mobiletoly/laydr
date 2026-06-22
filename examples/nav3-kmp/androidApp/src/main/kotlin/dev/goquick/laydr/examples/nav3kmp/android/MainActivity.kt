package dev.goquick.laydr.examples.nav3kmp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.goquick.laydr.examples.nav3kmp.Nav3KmpAddressBookApp

internal class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Nav3KmpAddressBookApp()
        }
    }
}
