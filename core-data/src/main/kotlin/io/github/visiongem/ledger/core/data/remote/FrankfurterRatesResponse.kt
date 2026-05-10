package io.github.visiongem.ledger.core.data.remote

import com.squareup.moshi.JsonClass

// Frankfurter API response shape (api.frankfurter.app/latest, /yyyy-MM-dd).
// Rates arrive as Double; ExchangeRateRepository converts to BigDecimal before persisting.
@JsonClass(generateAdapter = true)
data class FrankfurterRatesResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>,
)
