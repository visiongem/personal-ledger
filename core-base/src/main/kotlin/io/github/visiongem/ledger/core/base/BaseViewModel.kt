package io.github.visiongem.ledger.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    private val _errorFlow = MutableSharedFlow<Throwable>(extraBufferCapacity = 1)
    val errorFlow: SharedFlow<Throwable> = _errorFlow.asSharedFlow()

    protected fun launchCatching(
        onError: (Throwable) -> Unit = ::onUnhandledError,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = viewModelScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }

    protected fun launchOnIO(
        onError: (Throwable) -> Unit = ::onUnhandledError,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = viewModelScope.launch(Dispatchers.IO) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }

    protected open fun onUnhandledError(t: Throwable) {
        _errorFlow.tryEmit(t)
    }
}
