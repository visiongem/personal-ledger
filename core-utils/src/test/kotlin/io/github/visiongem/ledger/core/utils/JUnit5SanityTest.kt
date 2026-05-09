package io.github.visiongem.ledger.core.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class JUnit5SanityTest {
    @Test
    fun jupiterPlatformWorks() {
        assertThat(1 + 1).isEqualTo(2)
    }
}
