package io.github.visiongem.ledger.core.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseSmokeTest {

    private lateinit var db: AppDatabase

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    @Test
    fun roomInMemoryBuildsAndExposesAllDaos() {
        db = InMemoryDatabaseFactory.create()
        assertThat(db.accountDao()).isNotNull()
        assertThat(db.categoryDao()).isNotNull()
        assertThat(db.recordDao()).isNotNull()
        assertThat(db.budgetDao()).isNotNull()
        assertThat(db.exchangeRateDao()).isNotNull()
    }
}
