package consumer

import androidx.compose.runtime.compositionLocalOf
import consumer.generated.LaydrRoutes

internal val LocalConsumerGeneration = compositionLocalOf { 0 }

fun consumerSmoke(): Boolean = LaydrRoutes.Home.path() == "/home"
