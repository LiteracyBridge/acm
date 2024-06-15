package org.literacybridge.talkingbookapp

import AppNavHost
import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
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
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.compose.withSentryObservableEffect
import org.literacybridge.talkingbookapp.ui.theme.TalkingBookAppTheme
import org.literacybridge.talkingbookapp.util.Constants.Companion.LOG_TAG
import org.literacybridge.talkingbookapp.util.FileObserverWrapper
import org.literacybridge.talkingbookapp.util.content_manager.CustomS3PathResolver
import org.literacybridge.talkingbookapp.util.device_manager.Dfu
import org.literacybridge.talkingbookapp.util.device_manager.Usb
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel
import java.io.File


const val TAG = "TalkingBook";

@AndroidEntryPoint
class MainActivity : ComponentActivity(), Handler.Callback, Usb.OnUsbChangeListener {

    private val PERMISSION_WRITE_STORAGE_CODE = 12
    private val PERMISSION_READ_AUDIO_CODE = 13

    private lateinit var usb: Usb
//    private lateinit var dfu: Dfu
    private val talkingBookViewModel: TalkingBookViewModel by viewModels()

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

        // Manage external storage is available in Android 11+, request user to enable it
        // if we don't have it yet
        if (SDK_INT >= Build.VERSION_CODES.R) {
            // TODO: Improve the UX for this, maybe show a dialog explaining why we need it
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.setData(uri)
                startActivity(intent)
            }
        } else { // Below Android 11, we have to request for write external storage permission
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requestPermissions(permissions, PERMISSION_WRITE_STORAGE_CODE)
        }

        // Setup dfu
//        dfu = Dfu(Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID)
//        dfu.setListener(this)

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


//        todo: add usb manager code here
        // Get the USB manager service
        // Get the USB manager service
//        val usbManager = (getSystemService(USB_SERVICE) as UsbManager)!!
//
//        // Get a list of connected USB devices
//        for (usbDevice in usbManager!!.deviceList.values) {
//            // Check if the device matches the STM32Fx vendor and product IDs
//            if ( /*usbDevice.getVendorId() == 1155 &&*/usbDevice.productId == 57105) {
//                // Process the connected STM32 device
//                val deviceId = usbDevice.deviceId
//                val deviceName = usbDevice.deviceName
//                //                String deviceManufacturer = usbDevice.getManufacturerName();
////                String deviceProduct = usbDevice.getProductName();
//
//                // Perform actions based on the connected STM32 device
//                // ...
//
//                // Log or display information about the connected STM32 device
//                val deviceInfo = """
//        Device ID: $deviceId
//        Device Name: $deviceName
//        """.trimIndent()
//                //                        "\nManufacturer: " + deviceManufacturer +
////                        "\nProduct: " + deviceProduct;
//
//                // Log the STM32 device information
//                // You can also display this information in your app as needed
//                Log.d(LOG_TAG, "$deviceInfo")
//
//                usb = Usb(this)
//                usb.setUsbManager(usbManager)
//                usb.setOnUsbChangeListener(this)
//
//                // Handle two types of intents. Device attachment and permission
//                registerReceiver(usb.getmUsbReceiver(), IntentFilter(Usb.ACTION_USB_PERMISSION))
//                registerReceiver(
//                    usb.getmUsbReceiver(),
//                    IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
//                )
//                registerReceiver(
//                    usb.getmUsbReceiver(),
//                    IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
//                )
//                usb.setDevice(usbDevice)
//                dfu.setUsb(usb)
//                //                device = usbDevice;
//                Log.d("Device Finder", "device found$deviceInfo")
//                break
//            }
//        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()

        /* Setup USB */
        usb = Usb.getInstance()
        usb.setUsbManager(getSystemService(USB_SERVICE) as UsbManager)
        usb.setOnUsbChangeListener(this)

        // Handle two types of intents. Device attachment and permission
        registerReceiver(usb.getmUsbReceiver(), IntentFilter(Usb.ACTION_USB_PERMISSION), RECEIVER_EXPORTED)
        registerReceiver(usb.getmUsbReceiver(), IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED))
        registerReceiver(usb.getmUsbReceiver(), IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED))

        usb.requestPermission(this)
    }

    override fun onStop() {
        super.onStop()

        /* USB */
//        dfu.setUsb(null)
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

        Log.d(TAG, "$deviceInfo")
//        status.setText(deviceInfo)
//        dfu.setUsb(usb)
    }

    override fun onUsbDisconnected() {
//        dfu.setUsb(null)

        talkingBookViewModel.disconnected()
        usb.release()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_WRITE_STORAGE_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    Log.d(LOG_TAG, "storage permission granted")
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return
            }

            PERMISSION_READ_AUDIO_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    Log.d(LOG_TAG, "storage permission granted")
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }
}
