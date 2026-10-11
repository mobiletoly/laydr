package routes.home.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import consumer.LocalConsumerGeneration
import dev.goquick.laydr.compose.LaydrScreenContent
import dev.goquick.laydr.nav3.kmp.requireLaydrNavPayload
import dev.goquick.laydr.nav3.kmp.requireLaydrNavResultSink

internal val Route = LaydrRouteDef.screenWithLayoutValues(content = {
    val generation = LocalConsumerGeneration.current
    LaydrScreenContent {
        val payload = requireLaydrNavPayload<String>()
        val sink = requireLaydrNavResultSink<String>()
        Column {
            BasicText(payload)
            BasicText("generation $generation")
            BasicText("Complete", Modifier.clickable { sink.complete("accepted") })
        }
    }
})
