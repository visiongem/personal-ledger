package io.github.visiongem.ledger.core.extensions

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun keyboardAsState(): State<Boolean> = rememberUpdatedState(WindowInsets.isImeVisible)

@Composable
fun OnLifecycleEvent(onEvent: (Lifecycle.Event) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val callback by rememberUpdatedState(onEvent)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> callback(event) }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
