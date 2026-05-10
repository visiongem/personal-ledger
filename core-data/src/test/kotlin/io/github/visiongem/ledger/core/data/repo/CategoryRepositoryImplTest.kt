package io.github.visiongem.ledger.core.data.repo

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.local.dao.CategoryDao
import io.github.visiongem.ledger.core.data.local.entity.CategoryEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CategoryRepositoryImplTest {

    private val dao = mockk<CategoryDao>()
    private val repo: CategoryRepository = CategoryRepositoryImpl(dao)

    private val foodEntity = CategoryEntity(
        id = 1L,
        name = "Food",
        type = CategoryType.EXPENSE,
        iconKey = "food",
        sortOrder = 0,
    )

    @Test
    fun observeAllMapsToDomain() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(foodEntity))
        repo.observeAll().test {
            val list = awaitItem()
            assertThat(list).hasSize(1)
            assertThat(list[0].type).isEqualTo(CategoryType.EXPENSE)
            awaitComplete()
        }
    }

    @Test
    fun observeByTypeDelegatesAndMaps() = runTest {
        every { dao.observeByType(CategoryType.INCOME) } returns flowOf(emptyList())
        repo.observeByType(CategoryType.INCOME).test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }
    }

    @Test
    fun upsertConvertsDomainToEntity() = runTest {
        coEvery { dao.upsert(any()) } returns 5L
        val category = Category(
            id = 0L,
            name = "Transport",
            type = CategoryType.EXPENSE,
            iconKey = "transport",
            sortOrder = 1,
        )
        assertThat(repo.upsert(category)).isEqualTo(5L)
        coVerify { dao.upsert(match { it.name == "Transport" && it.type == CategoryType.EXPENSE }) }
    }

    @Test
    fun deleteByIdDelegatesToDao() = runTest {
        coEvery { dao.deleteById(any()) } returns Unit
        repo.deleteById(11L)
        coVerify { dao.deleteById(11L) }
    }
}
