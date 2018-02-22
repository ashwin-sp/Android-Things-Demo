package com.bhaicompany.androidthingsdemo.Retrofit

import android.util.Log
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.RequestBody
import org.json.JSONArray


/**
 * Created by ashwin-4529 on 18/02/18.
 */

object RetrofitCallBuilder: Callback<String>
{
    private lateinit var loggingInterceptor: HttpLoggingInterceptor
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var retrofitBuilder: Retrofit
    private lateinit var imageCallback: ImageCallback
    fun initRetroBuilder()
    {
        loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC
        okHttpClient = OkHttpClient.Builder()
                .readTimeout(40, TimeUnit.SECONDS)
                .connectTimeout(40, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()
        retrofitBuilder = Retrofit.Builder().baseUrl("https://www.googleapis.com/customsearch/")
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
    }
    fun getData(url: String, encodedImage: String, callback: ImageCallback)
    {
        val getAPI = retrofitBuilder.create(RetrofitCalls::class.java)
        logLargeString(encodedImage)
        imageCallback = callback
        val text = "{\n" +
                "  \"requests\":[\n" +
                "    {\n" +
                "      \"image\":{\n" +
                "        \"content\":\"$encodedImage\"\n" +
                "      },\n" +
                "      \"features\":[\n" +
                "        {\n" +
                "          \"type\":\"LABEL_DETECTION\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}"
        Log.d("Request body ", text)
/*        val text = "{\n" +
                "  \"requests\":[\n" +
                "    {\n" +
                "      \"image\":{\n" +
                "        \"source\":{\n" +
                "          \"imageUri\":\n" +
                "            \"$uri\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"features\":[\n" +
                "        {\n" +
                "          \"type\":\"FACE_DETECTION\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}"*/
        val body = RequestBody.create(MediaType.parse("text/plain"), text)
       // val requestBody = MainDataClass(arrayOf(Requests(arrayOf(Features("LABEL_DETECTION",1)), Image(encodedImage))))
        val call =  getAPI.getRetrofitResponseObject(url, body)
        try {
            call.enqueue(this)
        }catch (e: Exception)
        {
            e.printStackTrace()
        }
    }
    override fun onResponse(call: Call<String>?, response: Response<String>?) {
        System.out.println("Response Body ==> "+ response!!.body())
        if(response.code() == 200) {
            val jresponse = JSONObject(response.body())
            val items = jresponse.getJSONArray("responses")
            // val firstObject = items.getJSONObject(00
            imageCallback.updateImage(items)
        }
        imageCallback.updateImage(JSONArray())
    }

    override fun onFailure(call: Call<String>?, t: Throwable?) {

    }

    fun logLargeString(str: String) {
        if (str.length > 3000) {
            Log.i("encoded image ", str.substring(0, 3000))
            logLargeString(str.substring(3000))
        } else {
            Log.i("encoded image ", str) // continuation
        }
    }
}