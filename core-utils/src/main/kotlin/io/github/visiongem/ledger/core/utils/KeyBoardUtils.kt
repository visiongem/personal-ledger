package io.github.visiongem.ledger.core.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

private val View.imm: InputMethodManager?
    get() = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

fun View.showKeyBoard() {
    requestFocus()
    imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.hideKeyBoard() {
    imm?.hideSoftInputFromWindow(windowToken, 0)
}

fun Activity.toggleKeyBoard() {
    val anchor = currentFocus ?: window.decorView
    val controller = WindowCompat.getInsetsController(window, anchor)
    val visible = ViewCompat.getRootWindowInsets(anchor)
        ?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
    if (visible) controller.hide(WindowInsetsCompat.Type.ime())
    else controller.show(WindowInsetsCompat.Type.ime())
}
