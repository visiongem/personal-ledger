package io.github.visiongem.ledger.core.data.backup

import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType
import org.junit.jupiter.api.Test

class CategoryCsvTest {

    private val food = Category(
        id = 10L,
        name = "Food",
        type = CategoryType.EXPENSE,
        iconKey = "food",
        sortOrder = 0,
    )

    private val salary = Category(
        id = 20L,
        name = "Salary",
        type = CategoryType.INCOME,
        iconKey = null,
        sortOrder = 5,
    )

    @Test fun roundTripPreservesFields() {
        val csv = CategoryCsv.toCsv(listOf(food, salary))
        val parsed = CategoryCsv.fromCsv(csv)!!
        assertThat(parsed).containsExactly(food, salary).inOrder()
    }

    @Test fun nullIconKeyRoundTrips() {
        val csv = CategoryCsv.toCsv(listOf(salary))
        assertThat(CategoryCsv.fromCsv(csv)!![0].iconKey).isNull()
    }

    @Test fun nameWithSpecialCharsRoundTrips() {
        val tricky = food.copy(name = "Food, \"dine in\"")
        val csv = CategoryCsv.toCsv(listOf(tricky))
        assertThat(CategoryCsv.fromCsv(csv)).containsExactly(tricky)
    }

    @Test fun emptyListProducesHeaderOnly() {
        val csv = CategoryCsv.toCsv(emptyList())
        assertThat(csv.trim()).isEqualTo(CategoryCsv.HEADER)
        assertThat(CategoryCsv.fromCsv(csv)).isEmpty()
    }

    @Test fun missingHeaderReturnsNull() {
        assertThat(CategoryCsv.fromCsv("10,Food,EXPENSE,food,0\n")).isNull()
    }

    @Test fun unknownTypeReturnsNull() {
        val bad = "${CategoryCsv.HEADER}\n10,Food,GAS,food,0\n"
        assertThat(CategoryCsv.fromCsv(bad)).isNull()
    }
}
