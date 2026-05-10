package io.github.visiongem.ledger.core.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.io.Serializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// ===== 尺寸（dp/sp/px 与像素互转）=====

private val displayMetrics get() = Resources.getSystem().displayMetrics

val Int.dp: Int get() = (this * displayMetrics.density).toInt()
val Float.dp: Float get() = this * displayMetrics.density

@Suppress("DEPRECATION") // scaledDensity is the established px-conversion path; per-character font scale is out of scope.
val Int.sp: Int get() = (this * displayMetrics.scaledDensity).toInt()

@Suppress("DEPRECATION")
val Float.sp: Float get() = this * displayMetrics.scaledDensity

val Int.px: Int get() = this // identity helper, makes call sites self-documenting.

// ===== View 显隐 =====

fun View.gone() {
    visibility = View.GONE
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.toggleVisible(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

// ===== Context 派生 =====

fun Context.toast(msg: CharSequence, long: Boolean = false) {
    Toast.makeText(this, msg, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun Context.toast(@StringRes resId: Int, long: Boolean = false) {
    toast(getString(resId), long)
}

@ColorInt
fun Context.color(@ColorRes id: Int): Int = ContextCompat.getColor(this, id)

fun Context.string(@StringRes id: Int, vararg args: Any): String =
    if (args.isEmpty()) getString(id) else getString(id, *args)

fun Context.dimen(@DimenRes id: Int): Float = resources.getDimension(id)

// ===== Activity / Intent 启动 =====

inline fun <reified T : Activity> Context.startActivity(noinline extras: Intent.() -> Unit = {}) {
    val intent = Intent(this, T::class.java).apply(extras)
    startActivity(intent)
}

// Type-dispatched putExtra; rejects unknown types loudly rather than silently dropping the value.
fun Intent.putExtraSafe(key: String, value: Any?): Intent {
    when (value) {
        null -> {}
        is String -> putExtra(key, value)
        is Int -> putExtra(key, value)
        is Long -> putExtra(key, value)
        is Boolean -> putExtra(key, value)
        is Float -> putExtra(key, value)
        is Double -> putExtra(key, value)
        is CharSequence -> putExtra(key, value)
        is Serializable -> putExtra(key, value)
        else -> error("Unsupported extra type: ${value::class.simpleName}")
    }
    return this
}

// ===== Bitmap =====

fun Bitmap.toFile(
    file: File,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 90,
): Boolean = try {
    file.parentFile?.takeIf { !it.exists() }?.mkdirs()
    file.outputStream().use { out -> compress(format, quality, out) }
} catch (_: IOException) {
    false
}

// ===== Color =====

@JvmName("colorIntToHexString")
fun @receiver:ColorInt Int.toHexString(): String = "#%08X".format(this)

@ColorInt
fun String.toColorIntOrNull(): Int? = try {
    Color.parseColor(this)
} catch (_: IllegalArgumentException) {
    null
}

// ===== Flow =====

fun <T> Flow<T>.collectIn(scope: CoroutineScope, action: suspend (T) -> Unit): Job =
    scope.launch { collect { action(it) } }
