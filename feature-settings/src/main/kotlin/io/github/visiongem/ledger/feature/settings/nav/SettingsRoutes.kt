package io.github.visiongem.ledger.feature.settings.nav

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data object SettingsHomeRoute : Parcelable

@Parcelize
data object CategoryListRoute : Parcelable

@Parcelize
data class CategoryEditRoute(val categoryId: Long?) : Parcelable

@Parcelize
data object BudgetsRoute : Parcelable
