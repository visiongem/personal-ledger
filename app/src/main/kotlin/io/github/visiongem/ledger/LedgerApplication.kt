package io.github.visiongem.ledger

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import io.github.visiongem.ledger.work.ExchangeRateRefreshWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class LedgerApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleExchangeRateRefresh()
    }

    private fun scheduleExchangeRateRefresh() {
        val workManager = WorkManager.getInstance(this)

        // Kick once immediately so the rate cache is fresh from day one.
        workManager.enqueueUniqueWork(
            "${ExchangeRateRefreshWorker.UNIQUE_NAME}-initial",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ExchangeRateRefreshWorker>().build(),
        )

        // Recur every 24h. KEEP avoids reschedule on every cold start; WorkManager's
        // back-off handles transient failures within each iteration.
        workManager.enqueueUniquePeriodicWork(
            ExchangeRateRefreshWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ExchangeRateRefreshWorker>(24, TimeUnit.HOURS).build(),
        )
    }
}
