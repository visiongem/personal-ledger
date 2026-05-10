package io.github.visiongem.ledger.core.data.di

import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.visiongem.ledger.core.data.remote.FrankfurterApi
import io.github.visiongem.ledger.core.network.di.FrankfurterRetrofit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {

    private const val FRANKFURTER_BASE_URL = "https://api.frankfurter.app/"

    @Provides
    @Singleton
    @FrankfurterRetrofit
    fun provideFrankfurterRetrofit(
        client: OkHttpClient,
        moshi: Moshi,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(FRANKFURTER_BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideFrankfurterApi(
        @FrankfurterRetrofit retrofit: Retrofit,
    ): FrankfurterApi = retrofit.create(FrankfurterApi::class.java)
}
