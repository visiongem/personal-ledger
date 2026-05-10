package io.github.visiongem.ledger.core.utils

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.View
import androidx.annotation.DrawableRes

object SpanUtil {

    fun buildColored(
        text: String,
        ranges: List<Pair<IntRange, Int>>,
    ): SpannableStringBuilder {
        val builder = SpannableStringBuilder(text)
        ranges.forEach { (range, color) ->
            builder.setSpan(
                ForegroundColorSpan(color),
                range.first.coerceAtLeast(0),
                (range.last + 1).coerceAtMost(text.length),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        return builder
    }

    fun buildClickable(
        text: String,
        range: IntRange,
        onClick: () -> Unit,
    ): SpannableStringBuilder {
        val builder = SpannableStringBuilder(text)
        builder.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) = onClick()
            },
            range.first.coerceAtLeast(0),
            (range.last + 1).coerceAtMost(text.length),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        return builder
    }
}

fun SpannableStringBuilder.appendImage(
    context: Context,
    @DrawableRes drawableRes: Int,
    placeholder: String = " ",
    verticalAlignment: Int = ImageSpan.ALIGN_BASELINE,
): SpannableStringBuilder {
    val start = length
    append(placeholder)
    setSpan(
        ImageSpan(context, drawableRes, verticalAlignment),
        start,
        start + placeholder.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    return this
}
