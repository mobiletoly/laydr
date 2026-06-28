package dev.goquick.laydr.examples.nav3kmpshopping

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Creates the UIKit entry point used by the iOS shopping example launcher.
 */
fun MainViewController(): UIViewController =
    ComposeUIViewController {
        Nav3KmpShoppingApp()
    }
