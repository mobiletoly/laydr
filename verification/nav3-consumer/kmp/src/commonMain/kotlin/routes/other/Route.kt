package routes.other

import androidx.compose.foundation.text.BasicText
import dev.goquick.laydr.compose.LaydrScreenContent

internal val Route = LaydrRouteDef.screenWithLayoutValues(content = { LaydrScreenContent { BasicText("other") } })
