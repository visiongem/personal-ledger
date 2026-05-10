package io.github.visiongem.ledger.core.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FrankfurterApi {

    @GET("latest")
    suspend fun getLatest(
        @Query("from") base: String,
        @Query("to") symbols: String,
    ): FrankfurterRatesResponse

    // Path date format: ISO yyyy-MM-dd.
    @GET("{date}")
    suspend fun getOnDate(
        @Path("date") date: String,
        @Query("from") base: String,
        @Query("to") symbols: String,
    ): FrankfurterRatesResponse
}
