package io.github.visiongem.ledger.core.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class NumberUtilTest {

    // ===== toPercent =====
    @Test fun toPercentBasic() = assertThat("0.123".toPercent(2)).isEqualTo("12.30")
    @Test fun toPercentZero() = assertThat("0".toPercent(2)).isEqualTo("0.00")
    @Test fun toPercentTrimZero() = assertThat("0.5".toPercent(2, trimZero = true)).isEqualTo("50")
    @Test fun toPercentNullSafe() = assertThat((null as String?).toPercent(2)).isEqualTo("0.00")

    // ===== compareWith =====
    @Test fun compareEqual() = assertThat("1.0".compareWith("1.00")).isEqualTo(0)
    @Test fun compareGreater() = assertThat("2.0".compareWith("1.0")).isGreaterThan(0)
    @Test fun compareLess() = assertThat("1.0".compareWith("2.0")).isLessThan(0)
}
