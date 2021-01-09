package com.shaoman.customer.helper

import android.util.Log
import com.charonchui.cyberlink.util.LogUtil
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.shaoman.customer.model.entity.res.HttpResult
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

object JsonEntityGetter {

    val GSON: Gson = Gson()
    private var okHttpClient: OkHttpClient? = null

    @Synchronized
    private fun getOkhttpClient(): OkHttpClient {
        if (okHttpClient == null)
            okHttpClient = OkHttpClient.Builder()
                    .callTimeout(10_000L, TimeUnit.MILLISECONDS)
                    .connectTimeout(10_000L, TimeUnit.MILLISECONDS)
                    .addInterceptor(HttpLoggingInterceptor { msg ->
                        Log.e("HttpLog", msg)
                    })
                    .build()
        return okHttpClient!!
    }

    fun <T> getHttpResultJsonEntity(url: String, typeToken: TypeToken<*>, blocking: Function1<HttpResult<T>, Unit>) {
        try {
            val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
            val newCall = getOkhttpClient().newCall(request)
            newCall.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                }

                override fun onResponse(call: Call, response: Response) {
                    val code = response.code
                    if (code == 200) {
                        try {
                            val jsonStr = response.body?.string() ?: ""
                            val httpResult = GSON.fromJson<HttpResult<T>>(jsonStr, typeToken.type)
                            blocking.invoke(httpResult)
                        } catch (ex: Throwable) {
                            ex.printStackTrace()
                        }
                    }
                }
            })
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    fun <T> getJsonEntityWithToken(url: String, typeToken: TypeToken<*>, blocking: Function1<T, Unit>) {
        val request = Request.Builder()
                .url(url)
                .get()
                .build()
        val newCall = getOkhttpClient().newCall(request)
        newCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                if (code == 200) {
                    try {
                        val jsonStr = response.body?.string() ?: ""
                        val httpResult = GSON.fromJson<T>(jsonStr, typeToken.type)
                        blocking.invoke(httpResult)
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                    }
                }
            }
        })
    }

    private fun getPostParam(map: HashMap<String, Any>): RequestBody {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val jsonObject = JsonObject()
        for (a in map.entries) {
            val key = a.key
            val value = a.value
            if (value is String)
                jsonObject.addProperty(key, value)
            if (value is Number)
                jsonObject.addProperty(key, value)
            if (value is Boolean)
                jsonObject.addProperty(key, value)
            if (value is Char)
                jsonObject.addProperty(key, value)
        }
        return jsonObject.toString().toRequestBody(mediaType)
    }

    fun <T> postJsonEntity(url: String, map: HashMap<String, Any>, typeToken: TypeToken<*>, blocking: Function1<HttpResult<T>, Unit>) {
        val request = Request.Builder()
                .url(url)
                .post(getPostParam(map))
                .header("token", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiI5NiIsImV4cCI6MTYwNzU3ODgzNCwibmJmIjoxNjA2OTc0MDM0fQ.4nwgQQhTe7597HxYEwuzTFX0x72iwzbtQPnzuSLXdTA")
                .build()
        val newCall = getOkhttpClient().newCall(request)
        newCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                if (code == 200) {
                    try {
                        val jsonStr = response.body?.string() ?: ""
                        val httpResult = GSON.fromJson<HttpResult<T>>(jsonStr, typeToken.type)
                        blocking.invoke(httpResult)
                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                    }
                }
            }
        })
    }
}