package org.literacybridge.tbloaderandroid.api_services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import okio.buffer
import okio.sink
import org.literacybridge.tbloaderandroid.App
import org.literacybridge.tbloaderandroid.models.Release
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class AppUpdateHttpClient {
    private var client: OkHttpClient

    init {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC)

        this.client = OkHttpClient.Builder().addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()

//                .header("Accept", "application/json")
//                .header("Content-Type", "application/json")
//                .header("Authorization", "Bearer ${dataStoreManager.accessToken}")
                .method(original.method, original.body)

            val request = requestBuilder.build()
            chain.proceed(request)
        }
            .addInterceptor(logging)
            .writeTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .build()
    }

    fun getLastRelease(url: String): Release {
        val body = get(url)
        return Gson().fromJson(body, Release::class.java)
    }

    fun download(url: String, filename: String): File? {
        if (!isConnectedToInternet()) throw NoInternetConnectionException()

        var response: Response? = null
        try {
            response = client.newCall(Request.Builder().url(url).build()).execute()
        } catch (e: IOException) {
            if (e.javaClass == UnknownHostException::class.java) throw (e as UnknownHostException) else if (e.javaClass == InterruptedIOException::class.java) throw (e as InterruptedIOException)
            e.printStackTrace()
        }
        if (response != null && !response.isSuccessful) {
            throw HttpException(response.code)
        }

        try {
            val file =
                File(App.context.applicationContext.getFileStreamPath(filename).path)
            val sink: BufferedSink = file.sink().buffer()
            sink.writeAll(response!!.body.source())
            sink.flush()
            sink.close()

            return file
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }


    private operator fun get(url: String): String? {
        if (!isConnectedToInternet()) throw NoInternetConnectionException()

        var response: Response? = null
        try {
            response = client.newCall(Request.Builder().url(url).build()).execute()
        } catch (e: IOException) {
            if (e.javaClass == UnknownHostException::class.java) throw (e as UnknownHostException) else if (e.javaClass == InterruptedIOException::class.java) throw (e as InterruptedIOException)
            e.printStackTrace()
        }

        if (response != null && !response.isSuccessful) throw HttpException(response.code)

        val responseBody: ResponseBody = response!!.body ?: return null
        try {
            return responseBody.string()
        } catch (e: IOException) {
            // nothing
        }
        return null
    }

    fun isConnectedToInternet(): Boolean {
        val connectivity =
            App.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = connectivity.allNetworkInfo
        for (networkInfo in info) if (networkInfo.state == NetworkInfo.State.CONNECTED) {
            return true
        }
        return false
    }


    class NoInternetConnectionException : Exception()


    class HttpException(val code: Int) : Exception("HTTP Exception $code")

}
