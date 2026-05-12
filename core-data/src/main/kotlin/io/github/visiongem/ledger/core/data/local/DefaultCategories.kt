package io.github.visiongem.ledger.core.data.local

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Seed default categories on first database creation. Without these the
 * record-edit category picker is empty on a fresh install and the user
 * can't save any expense or income record. Names are seeded in Chinese
 * since this is a personal app; the user can rename them later once a
 * category management screen exists.
 */
internal object DefaultCategories {

    private val expenseSeeds = listOf(
        "餐饮", "交通", "购物", "居住", "娱乐", "医疗", "教育", "其他",
    )

    private val incomeSeeds = listOf(
        "工资", "奖金", "投资", "其他",
    )

    fun seed(db: SupportSQLiteDatabase) {
        expenseSeeds.forEachIndexed { index, name ->
            db.execSQL(
                "INSERT INTO category(name, type, iconKey, sortOrder) VALUES(?, ?, NULL, ?)",
                arrayOf<Any>(name, "EXPENSE", index + 1),
            )
        }
        incomeSeeds.forEachIndexed { index, name ->
            db.execSQL(
                "INSERT INTO category(name, type, iconKey, sortOrder) VALUES(?, ?, NULL, ?)",
                arrayOf<Any>(name, "INCOME", index + 1),
            )
        }
    }
}
