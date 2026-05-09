package io.github.visiongem.ledger.core.utils

import java.math.BigDecimal
import java.math.RoundingMode

private val ONE_HUNDRED = BigDecimal(100)

fun String?.toPercent(scale: Int, trimZero: Boolean = false): String {
    val percent = this.toSafeBigDecimal()
        .multiply(ONE_HUNDRED)
        .setScale(scale, RoundingMode.HALF_UP)
    return if (trimZero) percent.stripTrailingZeros().toPlainString() else percent.toPlainString()
}

fun String?.compareWith(other: String?): Int =
    this.toSafeBigDecimal().compareTo(other.toSafeBigDecimal())
