package io.github.visiongem.ledger.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.visiongem.ledger.core.data.local.AppDatabase
import io.github.visiongem.ledger.core.data.local.DefaultCategories
import io.github.visiongem.ledger.core.data.local.dao.AccountDao
import io.github.visiongem.ledger.core.data.local.dao.BudgetDao
import io.github.visiongem.ledger.core.data.local.dao.CategoryDao
import io.github.visiongem.ledger.core.data.local.dao.ExchangeRateDao
import io.github.visiongem.ledger.core.data.local.dao.RecordDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context = context,
        klass = AppDatabase::class.java,
        name = AppDatabase.DATABASE_NAME,
    )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                DefaultCategories.seed(db)
            }
        })
        .build()

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideRecordDao(db: AppDatabase): RecordDao = db.recordDao()

    @Provides
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideExchangeRateDao(db: AppDatabase): ExchangeRateDao = db.exchangeRateDao()
}
