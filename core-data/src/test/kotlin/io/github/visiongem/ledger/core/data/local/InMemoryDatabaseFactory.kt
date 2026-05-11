package io.github.visiongem.ledger.core.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

// FK constraints are off by default on every fresh SQLite connection; toggle them on
// so RESTRICT / SET_NULL / CASCADE behaviour matches production.
object InMemoryDatabaseFactory {
    fun create(): AppDatabase {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        db.openHelper.writableDatabase.setForeignKeyConstraintsEnabled(true)
        return db
    }
}
