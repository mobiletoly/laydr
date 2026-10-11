package routes.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import consumer.LocalConsumerGeneration
import dev.goquick.laydr.compose.LaydrScreenContent

internal val Route = LaydrRouteDef.screenWithLayoutValues(content = {
    val generation = LocalConsumerGeneration.current
    LaydrScreenContent {
        Column {
            BasicText("home")
            BasicText("generation $generation")
        }
    }
})
