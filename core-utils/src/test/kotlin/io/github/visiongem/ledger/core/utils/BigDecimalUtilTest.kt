package io.github.visiongem.ledger.core.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BigDecimalUtilTest {

    // ===== 加法 =====
    @Test fun addBasic() = assertThat("1.1".add("2.2")).isEqualTo("3.3")
    @Test fun addNullLeft() = assertThat((null as String?).add("1.5")).isEqualTo("1.5")
    @Test fun addNullRight() = assertThat("1.5".add(null)).isEqualTo("1.5")
    @Test fun addBothNull() = assertThat((null as String?).add(null)).isEqualTo("0")
    @Test fun addEmptyTreatedAsZero() = assertThat("".add("3.14")).isEqualTo("3.14")

    // ===== 减法 =====
    @Test fun subBasic() = assertThat("5.5".sub("2.2")).isEqualTo("3.3")
    @Test fun subNegativeResult() = assertThat("1.0".sub("3.0")).isEqualTo("-2.0")

    // ===== 乘法 =====
    @Test fun mulBasic() = assertThat("2.5".mul("4")).isEqualTo("10.0")
    @Test fun mulZero() = assertThat("3.14".mul("0")).isEqualTo("0.00")

    // ===== 除法 =====
    @Test fun divBasic() = assertThat("10".div("4", 2)).isEqualTo("2.50")
    @Test fun divPrecision() = assertThat("1".div("3", 6)).isEqualTo("0.333333")
    @Test fun divByZero() = assertThat("5".div("0", 2)).isEqualTo("0")

    // ===== 舍入 =====
    @Test fun roundDown() = assertThat("1.999".roundDown(2)).isEqualTo("1.99")
    @Test fun roundHalfUp() = assertThat("1.235".roundHalfUp(2)).isEqualTo("1.24")
    @Test fun roundHalfUpFiveDown() = assertThat("1.234".roundHalfUp(2)).isEqualTo("1.23")

    // ===== 去末尾零 =====
    @Test fun trimZeroSimple() = assertThat("1.2300".trimZero()).isEqualTo("1.23")
    @Test fun trimZeroAllZeros() = assertThat("1.000".trimZero()).isEqualTo("1")
    @Test fun trimZeroIntegerUnchanged() = assertThat("100".trimZero()).isEqualTo("100")

    // ===== 比较 =====
    @Test fun gtTrue() = assertThat("3.14".gt("3.13")).isTrue()
    @Test fun gtFalse() = assertThat("3.14".gt("3.15")).isFalse()
    @Test fun gtEqual() = assertThat("3.14".gt("3.14")).isFalse()
    @Test fun ltTrue() = assertThat("1".lt("2")).isTrue()
}
