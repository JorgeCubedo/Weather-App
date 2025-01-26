package com.cubo.weatherapp

import com.cubo.weatherapp.models.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("data/2.5/weather")
    fun getWeather(@Query("q") query: String, @Query("appid") appId: String): Call<WeatherResponse>
}