package org.literacybridge.tbloaderandroid

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.HiltAndroidApp
import org.literacybridge.core.spec.ProgramSpec
import org.literacybridge.tbloaderandroid.database.AppDatabase
import org.literacybridge.tbloaderandroid.database.ProgramContentEntity
import org.literacybridge.tbloaderandroid.util.PathsProvider
import java.io.File


@HiltAndroidApp
class App : Application() {
    val db by lazy { AppDatabase.getDatabase() }

    var programSpec: ProgramSpec? = null
        private set

    var programContent: ProgramContentEntity? = null
        private set

    override fun onCreate() {
        super.onCreate()
        application = this
    }

    companion object {
        var application: Application? = null
            private set

        val context: Context
            get() = application!!.applicationContext

        @Volatile
        private var instance: App? = null // Volatile modifier is necessary

        fun getInstance() =
            instance ?: synchronized(this) { // synchronized to avoid concurrency problem
                instance ?: App().also { instance = it }
            }
    }

    fun setProgramSpec(project: ProgramContentEntity): ProgramSpec? {
        this.programContent = project

        if (programSpec == null) {
            val progspecDir: File = PathsProvider.getProgramSpecDir(project)
            programSpec = ProgramSpec(progspecDir)
        }
        return programSpec
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

