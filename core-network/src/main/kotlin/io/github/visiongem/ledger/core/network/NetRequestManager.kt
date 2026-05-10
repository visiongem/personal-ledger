package io.github.visiongem.ledger.core.network

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object NetRequestManager {

    fun buildOkHttpClient(loggingEnabled: Boolean = false): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(ContentTypeInterceptor())
        if (loggingEnabled) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }
        return builder.build()
    }

    fun buildMoshi(): Moshi = Moshi.Builder().build()

    fun buildRetrofit(
        baseUrl: String,
        client: OkHttpClient,
        moshi: Moshi,
        successCode: Int = ApiResponse.SUCCESS_CODE,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(ApiResponseConverterFactory.create(moshi, successCode))
        .build()
}
