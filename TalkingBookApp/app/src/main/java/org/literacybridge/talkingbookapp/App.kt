package org.literacybridge.talkingbookapp

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.Room
//import com.amazonaws.mobileconnectors.s3.transferutility.TransferService
import dagger.hilt.android.HiltAndroidApp
import org.literacybridge.talkingbookapp.database.AppDatabase
import org.literacybridge.talkingbookapp.util.Config


@HiltAndroidApp
class App : Application() {
    var config: Config? = null
        private set

    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        application = this

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "tbloader_db"
        ).build()

        config = Config(this)
    }

    companion object {
        var  application: Application? = null
            private set

        val context: Context
            get() = application!!.applicationContext
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

            // for other device how are able to connect with Ethernet
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true

            // for check internet over Bluetooth
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }
}