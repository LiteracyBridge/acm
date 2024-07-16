package org.literacybridge.androidtbloader

import AppNavHost
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.FileObserver
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.plugin.Plugin
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.amplifyframework.storage.s3.configuration.AWSS3StoragePluginConfiguration
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.compose.withSentryObservableEffect
import org.literacybridge.androidtbloader.ui.theme.TalkingBookAppTheme
import org.literacybridge.androidtbloader.util.Constants.Companion.LOG_TAG
import org.literacybridge.androidtbloader.util.FileObserverWrapper
import org.literacybridge.androidtbloader.util.content_manager.CustomS3PathResolver
import org.literacybridge.androidtbloader.util.device_manager.Usb
import org.literacybridge.androidtbloader.view_models.TalkingBookViewModel
import java.io.File


const val TAG = "TalkingBook";

@AndroidEntryPoint
class MainActivity : ComponentActivity(), Handler.Callback, Usb.OnUsbChangeListener {
    private lateinit var usb: Usb
    private val talkingBookViewModel: TalkingBookViewModel by viewModels()
    private lateinit var appUpdateManager: AppUpdateManager

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                Toast.makeText(
                    this,
                    "Sorry, an error occurred updating the app",
                    Toast.LENGTH_LONG
                ).show()
            }
            Log.d(LOG_TAG, "${result.resultCode}")
            Log.d(LOG_TAG, "${result.data}")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.addPlugin<Plugin<*>>(
                AWSS3StoragePlugin(
                    AWSS3StoragePluginConfiguration {
                        awsS3PluginPrefixResolver = CustomS3PathResolver()
                    }
                )
            )
            Amplify.configure(applicationContext)
            Amplify.Logging.enable()
        } catch (e: Amplify.AlreadyConfiguredException) {
            Log.i(LOG_TAG, "Amplify plugin already configured")
        }

        setContent {
            val navController = rememberNavController().withSentryObservableEffect(
                enableNavigationBreadcrumbs = true,
                enableNavigationTracing = true
            )

            TalkingBookAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(navController = navController)
                }
            }
        }

        // Set up a file observer to watch the mass storage path, to detect when the device
        // is ready for writes
        val observer =
            object : FileObserverWrapper(
                Usb.MASS_STORAGE_PATH + "/",
                FileObserver.CREATE + FileObserver.CLOSE_WRITE + FileObserver.MODIFY
            ) {
                override fun onEvent(event: Int, path: String?) {
                    talkingBookViewModel.isMassStorageReady.value =
                        path?.let { File(it).exists() } == true
                    Log.d(LOG_TAG, "Device ready in mass storage mode")
                }
            }
        observer.startWatching()

        // Initialize AppUpdateManager
        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForAppUpdate()
    }

    override fun onResume() {
        super.onResume()
        resumeAppUpdate()
    }

    override fun onStart() {
        super.onStart()

        /* Setup USB */
        usb = Usb.getInstance()
        usb.setUsbManager(getSystemService(USB_SERVICE) as UsbManager)
        usb.setOnUsbChangeListener(this)

        // Handle two types of intents. Device attachment and permission
        if (SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(
                usb.getmUsbReceiver(),
                IntentFilter(Usb.ACTION_USB_PERMISSION),
                RECEIVER_EXPORTED
            )
        }

        registerReceiver(usb.getmUsbReceiver(), IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED))
        registerReceiver(usb.getmUsbReceiver(), IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED))
        usb.requestPermission(this);

        resumeAppUpdate()
    }

    override fun onStop() {
        super.onStop()

        talkingBookViewModel.disconnected()
        usb.release()

        try {
            unregisterReceiver(usb.getmUsbReceiver())
        } catch (e: IllegalArgumentException) {
            /* Already unregistered */
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        return false
    }

    override fun onUsbConnected() {
        talkingBookViewModel.setDevice(usb.usbDevice, usb.talkingBookDevice)
        talkingBookViewModel.setUsb(usb)

        val deviceInfo = usb.getDeviceInfo(usb.usbDevice)
        Log.d(TAG, deviceInfo)
    }

    override fun onUsbDisconnected() {
        talkingBookViewModel.disconnected()
        usb.release()
    }

    /**
     * If an in-app update is already running, resume the update.
     */
    private fun resumeAppUpdate() {
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activityResultLauncher,
                        AppUpdateOptions.newBuilder(IMMEDIATE).build()
                    )
                }
            }
    }

    private fun checkForAppUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(
                    IMMEDIATE
                )
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activityResultLauncher,
                    AppUpdateOptions.newBuilder(IMMEDIATE).build()
                )
            }
        }
    }
}
