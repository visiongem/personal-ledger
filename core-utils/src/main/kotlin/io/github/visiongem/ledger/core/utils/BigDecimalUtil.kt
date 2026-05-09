package io.github.visiongem.ledger.core.utils

import java.math.BigDecimal
import java.math.RoundingMode

private fun String?.toSafeBigDecimal(): BigDecimal {
    if (this.isNullOrBlank()) return BigDecimal.ZERO
    return try {
        BigDecimal(this)
    } catch (_: NumberFormatException) {
        BigDecimal.ZERO
    }
}

fun String?.add(other: String?): String =
    this.toSafeBigDecimal().add(other.toSafeBigDecimal()).toPlainString()

fun String?.sub(other: String?): String =
    this.toSafeBigDecimal().subtract(other.toSafeBigDecimal()).toPlainString()

fun String?.mul(other: String?): String =
    this.toSafeBigDecimal().multiply(other.toSafeBigDecimal()).toPlainString()

fun String?.div(other: String?, scale: Int): String {
    val divisor = other.toSafeBigDecimal()
    if (divisor.signum() == 0) return "0"
    return this.toSafeBigDecimal()
        .divide(divisor, scale, RoundingMode.HALF_UP)
        .toPlainString()
}

fun String?.roundDown(scale: Int): String =
    this.toSafeBigDecimal().setScale(scale, RoundingMode.DOWN).toPlainString()

fun String?.roundHalfUp(scale: Int): String =
    this.toSafeBigDecimal().setScale(scale, RoundingMode.HALF_UP).toPlainString()

// stripTrailingZeros may yield "1E+2" for "100"; toPlainString restores positional notation.
fun String?.trimZero(): String =
    this.toSafeBigDecimal().stripTrailingZeros().toPlainString()

fun String?.gt(other: String?): Boolean =
    this.toSafeBigDecimal().compareTo(other.toSafeBigDecimal()) > 0

fun String?.lt(other: String?): Boolean =
    this.toSafeBigDecimal().compareTo(other.toSafeBigDecimal()) < 0

fun String?.ge(other: String?): Boolean =
    this.toSafeBigDecimal().compareTo(other.toSafeBigDecimal()) >= 0

fun String?.le(other: String?): Boolean =
    this.toSafeBigDecimal().compareTo(other.toSafeBigDecimal()) <= 0

fun String?.eq(other: String?): Boolean =
    this.toSafeBigDecimal().compareTo(other.toSafeBigDecimal()) == 0
