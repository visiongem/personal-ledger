package io.github.visiongem.ledger.core.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class LargeNumberFormatUtilTest {

    @Test fun belowK() = assertThat(LargeNumberFormatUtil.format("999")).isEqualTo("999")
    @Test fun toK() = assertThat(LargeNumberFormatUtil.format("1500")).isEqualTo("1.5K")
    @Test fun toM() = assertThat(LargeNumberFormatUtil.format("1500000")).isEqualTo("1.5M")
    @Test fun toG() = assertThat(LargeNumberFormatUtil.format("1500000000")).isEqualTo("1.5G")
    @Test fun toT() = assertThat(LargeNumberFormatUtil.format("1500000000000")).isEqualTo("1.5T")
    @Test fun toP() = assertThat(LargeNumberFormatUtil.format("1500000000000000")).isEqualTo("1.5P")

    @Test fun zero() = assertThat(LargeNumberFormatUtil.format("0")).isEqualTo("0")
    @Test fun negative() = assertThat(LargeNumberFormatUtil.format("-1500")).isEqualTo("-1.5K")

    // 整数 K/M/... 不显示 .0（spec: "1000" → "1K"）
    @Test fun integerKTrimsDotZero() = assertThat(LargeNumberFormatUtil.format("1000")).isEqualTo("1K")
}
