package io.github.visiongem.ledger.core.utils

import android.view.View

private const val DEFAULT_INTERVAL_MS = 500L

// Per-View timestamp via setTag(R.id.*) so debounce state survives RecyclerView re-binds.
fun View.setOnSingleClickListener(
    intervalMs: Long = DEFAULT_INTERVAL_MS,
    block: (View) -> Unit,
) {
    setOnClickListener {
        val now = System.currentTimeMillis()
        val last = getTag(R.id.single_click_timestamp) as? Long ?: 0L
        if (now - last >= intervalMs) {
            setTag(R.id.single_click_timestamp, now)
            block(this)
        }
    }
}
