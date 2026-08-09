package dev.goquick.laydr.examples.nav3kmpshopping.android

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.goquick.laydr.nav3.kmp.LaydrNavStackNavigator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RetainedEntryStateRecreationTest {
    @Test
    fun payloadAndResultSurviveActivityRecreationWithRetainedOwner() {
        ActivityScenario.launch(RetainedEntryStateActivity::class.java).use { scenario ->
            scenario.awaitPayload("configuration-challenge")
            lateinit var staleNavigator: LaydrNavStackNavigator
            scenario.onActivity { activity ->
                assertNotNull(activity.currentNavigator)
                staleNavigator = checkNotNull(activity.currentNavigator)
                assertEquals("configuration-challenge", activity.visiblePayload)
            }

            scenario.recreate()
            scenario.awaitPayload("configuration-challenge")
            scenario.onActivity { activity ->
                val staleFailure = runCatching {
                    staleNavigator.push(HomeDestination)
                }.exceptionOrNull()
                assertTrue(staleFailure is IllegalStateException)
                val sink = activity.visibleResultSink
                assertNotNull(sink)
                checkNotNull(sink)
                sink.complete("accepted")
                sink.complete("ignored")

                assertEquals(1, activity.model.resultCount)
                assertEquals("accepted", activity.model.lastResult)
                assertEquals(0, activity.model.cancelCount)
            }
        }
    }

    private fun ActivityScenario<RetainedEntryStateActivity>.awaitPayload(expected: String) {
        repeat(100) {
            var payload: String? = null
            onActivity { activity -> payload = activity.visiblePayload }
            if (payload == expected) {
                return
            }
            Thread.sleep(20)
        }
        onActivity { activity -> assertEquals(expected, activity.visiblePayload) }
    }
}
