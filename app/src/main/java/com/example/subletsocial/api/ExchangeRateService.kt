package com.example.subletsocial.api

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApi {
    @GET("v6/latest/{base}")
    fun getLatestRates(@Path("base") base: String): Call<ExchangeRateResponse>
}

object ExchangeRateClient {
    private const val BASE_URL = "https://open.er-api.com/"

    val service: ExchangeRateApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExchangeRateApi::class.java)
    }
}
