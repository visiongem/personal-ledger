package io.github.visiongem.ledger.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepository
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.github.visiongem.ledger.core.data.repo.ExchangeRateRepository
import kotlinx.coroutines.flow.first

@HiltWorker
class ExchangeRateRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val accountRepository: AccountRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = userPreferencesRepository.flow.first()
        val others = accountRepository.observeAll().first()
            .map { it.currencyCode }
            .filter { it != prefs.defaultCurrency }
            .distinct()
        if (others.isEmpty()) return Result.success()

        val refresh = exchangeRateRepository.refreshLatest(prefs.defaultCurrency, others)
        return if (refresh.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "exchange-rate-refresh"
    }
}
