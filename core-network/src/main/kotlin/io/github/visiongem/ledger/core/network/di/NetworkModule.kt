package io.github.visiongem.ledger.core.network.di

import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.visiongem.ledger.core.network.NetRequestManager
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Placeholder until v1 wires a real backend. Other Retrofit instances (e.g. Frankfurter
    // raw API) live in their consuming module's @Module behind their own @Qualifier.
    private const val DEFAULT_BASE_URL = "https://example.invalid/"

    // TODO: switch to BuildConfig.DEBUG once the app module surfaces buildConfig fields.
    private const val DEFAULT_LOGGING_ENABLED = true

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = NetRequestManager.buildMoshi()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        NetRequestManager.buildOkHttpClient(loggingEnabled = DEFAULT_LOGGING_ENABLED)

    @Provides
    @Singleton
    @LedgerRetrofit
    fun provideLedgerRetrofit(
        client: OkHttpClient,
        moshi: Moshi,
    ): Retrofit = NetRequestManager.buildRetrofit(
        baseUrl = DEFAULT_BASE_URL,
        client = client,
        moshi = moshi,
    )
}
