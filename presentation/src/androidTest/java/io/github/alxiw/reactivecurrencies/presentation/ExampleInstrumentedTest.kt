package io.github.alxiw.reactivecurrencies.presentation

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
internal class ExampleInstrumentedTest {
    @Test
    internal fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.github.alxiw.reactivecurrencies.presentation.test", appContext.packageName)
    }
}
