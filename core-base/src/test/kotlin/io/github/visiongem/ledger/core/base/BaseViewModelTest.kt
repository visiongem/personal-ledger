package io.github.visiongem.ledger.core.base

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    @BeforeEach
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private class Probe : BaseViewModel() {
        fun runWork(block: suspend () -> Unit): Job = launchCatching { block() }
        fun runWithHandler(onError: (Throwable) -> Unit, block: suspend () -> Unit): Job =
            launchCatching(onError = onError) { block() }
    }

    @Test
    fun launchCatchingDoesNotEmitWhenBlockSucceeds() = runTest {
        val vm = Probe()
        vm.errorFlow.test {
            vm.runWork { /* nothing */ }
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun launchCatchingEmitsThrowableToErrorFlow() = runTest {
        val vm = Probe()
        val cause = RuntimeException("boom")
        vm.errorFlow.test {
            vm.runWork { throw cause }
            assertThat(awaitItem()).isSameInstanceAs(cause)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun customOnErrorBypassesDefaultErrorFlow() = runTest {
        val vm = Probe()
        var captured: Throwable? = null
        val cause = IllegalStateException("custom")
        vm.errorFlow.test {
            vm.runWithHandler(onError = { captured = it }) { throw cause }
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(captured).isSameInstanceAs(cause)
    }
}
