package com.bhaicompany.androidthingsdemo.Retrofit

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Created by ashwin-4529 on 18/02/18.
 */

interface RetrofitCalls
{
    @POST
    fun getRetrofitResponseObject(@Url url: String, @Body body: RequestBody): Call<String>
}