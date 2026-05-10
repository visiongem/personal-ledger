package io.github.visiongem.ledger.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.github.visiongem.ledger.core.data.repo.AccountRepositoryImpl
import io.github.visiongem.ledger.core.data.repo.BudgetRepository
import io.github.visiongem.ledger.core.data.repo.BudgetRepositoryImpl
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
import io.github.visiongem.ledger.core.data.repo.CategoryRepositoryImpl
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepository
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepositoryImpl
import io.github.visiongem.ledger.core.data.repo.ExchangeRateRepository
import io.github.visiongem.ledger.core.data.repo.ExchangeRateRepositoryImpl
import io.github.visiongem.ledger.core.data.repo.RecordRepository
import io.github.visiongem.ledger.core.data.repo.RecordRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindRecordRepository(impl: RecordRepositoryImpl): RecordRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindExchangeRateRepository(
        impl: ExchangeRateRepositoryImpl,
    ): ExchangeRateRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: UserPreferencesRepositoryImpl,
    ): UserPreferencesRepository
}
