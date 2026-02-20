package com.example.subletsocial.features

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.subletsocial.api.ExchangeRateClient
import com.example.subletsocial.api.ExchangeRateResponse
import com.example.subletsocial.model.Listing
import com.example.subletsocial.model.Model
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedViewModel : ViewModel() {
    val listings: LiveData<List<Listing>> = Model.shared.getAllListings()

    private val _exchangeRates = MutableLiveData<Map<String, Double>>()
    val exchangeRates: LiveData<Map<String, Double>> = _exchangeRates

    fun fetchExchangeRates(baseCurrency: String = "USD") {
        ExchangeRateClient.service.getLatestRates(baseCurrency).enqueue(object : Callback<ExchangeRateResponse> {
            override fun onResponse(call: Call<ExchangeRateResponse>, response: Response<ExchangeRateResponse>) {
                if (response.isSuccessful) {
                    _exchangeRates.postValue(response.body()?.rates)
                }
            }

            override fun onFailure(call: Call<ExchangeRateResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
}