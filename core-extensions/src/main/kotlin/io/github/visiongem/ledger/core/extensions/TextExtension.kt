package io.github.visiongem.ledger.core.extensions

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt

fun String.toSpannable(): SpannableStringBuilder = SpannableStringBuilder(this)

private fun IntRange.clampTo(length: Int): Pair<Int, Int> =
    first.coerceAtLeast(0) to (last + 1).coerceAtMost(length)

fun SpannableStringBuilder.colorRange(
    range: IntRange,
    @ColorInt color: Int,
): SpannableStringBuilder = apply {
    val (start, end) = range.clampTo(length)
    setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}

fun SpannableStringBuilder.boldRange(range: IntRange): SpannableStringBuilder = apply {
    val (start, end) = range.clampTo(length)
    setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}

fun SpannableStringBuilder.clickRange(
    range: IntRange,
    onClick: (View) -> Unit,
): SpannableStringBuilder = apply {
    val (start, end) = range.clampTo(length)
    setSpan(
        object : ClickableSpan() {
            override fun onClick(widget: View) = onClick(widget)
        },
        start,
        end,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
}

fun TextView.setSpannedText(builder: SpannableStringBuilder) {
    text = builder
    movementMethod = LinkMovementMethod.getInstance()
}
