package org.literacybridge.talkingbookapp

import AppNavHost
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import org.literacybridge.talkingbookapp.helpers.LOG_TAG
import org.literacybridge.talkingbookapp.helpers.dfu.Dfu
import org.literacybridge.talkingbookapp.helpers.dfu.Usb
import org.literacybridge.talkingbookapp.ui.theme.TalkingBookAppTheme
import org.literacybridge.talkingbookapp.view_models.TalkingBookViewModel


const val TAG = "TalkingBook";

class MainActivity : ComponentActivity(), Handler.Callback, Usb.OnUsbChangeListener,
    Dfu.DfuListener {

    private lateinit var usb: Usb
    private lateinit var dfu: Dfu
//    private val  talkingBookViewModel = TalkingBookViewModel();
    private val talkingBookViewModel: TalkingBookViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(applicationContext)
        } catch (e: Amplify.AlreadyConfiguredException) {
            Log.i(LOG_TAG, "Amplify plugin already configured")
        }

        // Setup dfu
        dfu = Dfu(Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID)
        dfu.setListener(this)

        // Setup view models
//        val viewModel: TalkingBookViewModel by viewModels()
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                talkingBookViewModel = viewModel
//
//                viewModel.deviceState.collect {
////                    // Update UI elements
//                }
//            }
//        }



        // TODO: implement permission write request
//        val permissions = arrayOf<String>(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//        requestPermissions(permissions, 12)

        setContent {
            TalkingBookAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(navController = rememberNavController())
//                    Greeting("Android")
//                    Authenticator(
//                        headerContent = {
//                            Box(
////                                modifier = Modifier.size(80.dp).align(Alignment.CenterHorizontally)
//                            ) {
//                                Image(
//                                    painter = painterResource(R.drawable.amplio_logo),
//                                    contentDescription = null,
//                                )
//                            }
//                        },
////                        footerContent = {
////                            Text(
////                                "© All Rights Reserved",
////                                modifier = Modifier.align(Alignment.CenterHorizontally)
////                            )
////                        }
//                    ) { state ->
//                        SignedInContent(state)
//                    }
                }
            }
        }

//        Amplify.Auth.getCurrentUser({
//            Log.i(TAG, "session = $it")
//        }, {
//            Log.e(TAG, "auth error = $it")
//        })

//            .initialize(applicationContext, object : Callback<UserStateDetails?>() {
//                fun onResult(userStateDetails: UserStateDetails) {
//                    Log.i(TAG, userStateDetails.getUserState().toString())
//                    when (userStateDetails.getUserState()) {
//                        SIGNED_IN -> {
//                            val i = Intent(this@AuthenticationActivity, MainActivity::class.java)
//                            startActivity(i)
//                        }
//
//                        SIGNED_OUT -> showSignIn()
//                        else -> {
//                            AWSMobileClient.getInstance().signOut()
//                            showSignIn()
//                        }
//                    }
//                }
//
//                fun onError(e: Exception) {
//                    Log.e(TAG, e.toString())
//                }
//            })

//        Amplify.Auth.fetchAuthSession(
//            { it ->
//                Log.i(TAG, it.isSignedIn.toString())
//            },
////            { Log.i("AmplifyQuickstart", "Auth session = $it") },
//            { error -> Log.e("AmplifyQuickstart", "Failed to fetch auth session", error) }
//        )


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


    override fun onStart() {
        super.onStart()

        /* Setup USB */
        usb = Usb(this)
        usb.setUsbManager(getSystemService(USB_SERVICE) as UsbManager)
        usb.setOnUsbChangeListener(this)

        // Handle two types of intents. Device attachment and permission
        registerReceiver(usb.getmUsbReceiver(), IntentFilter(Usb.ACTION_USB_PERMISSION))
        registerReceiver(usb.getmUsbReceiver(), IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED))
        registerReceiver(usb.getmUsbReceiver(), IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED))

        usb.requestPermission(this, Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID)
    }

    override fun onStop() {
        super.onStop()

        /* USB */
        dfu.setUsb(null)
        talkingBookViewModel.disconnected()

        usb.release()
        try {
            unregisterReceiver(usb.getmUsbReceiver())
        } catch (e: IllegalArgumentException) { /* Already unregistered */
        }
    }


    override fun onStatusMsg(msg: String?) {
        // TODO since we are appending we should make the TextView scrollable like a log
//        status.append(msg)
        Log.d(TAG, "$msg")

    }

    override fun handleMessage(msg: Message): Boolean {
        return false
    }

    override fun onUsbConnected() {
        talkingBookViewModel.setDevice(usb.usbDevice)

        val deviceInfo = usb.getDeviceInfo(usb.usbDevice)
        Log.d(TAG, "$deviceInfo")
//        status.setText(deviceInfo)
        dfu.setUsb(usb)
    }

    override fun onUsbDisconnected() {
        dfu.setUsb(null)

        talkingBookViewModel.disconnected()
        usb.release()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            12 -> {
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
