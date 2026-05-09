package io.github.visiongem.ledger

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltSmokeTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Test
    fun hiltContainerStarts() {
        hiltRule.inject()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(context)
    }
}
