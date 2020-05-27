package com.didahdx.smsgatewaysync.data.network

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

private const val BASE_URL =  "http://128.199.174.204/"

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .build()

interface SmsApiService {
    @POST
    fun postSms(@Url url:String,@Body post: PostSms): Call<PostSms>
}

object SmsApi {
    val retrofitService: SmsApiService by lazy {retrofit.create(SmsApiService::class.java)}
}