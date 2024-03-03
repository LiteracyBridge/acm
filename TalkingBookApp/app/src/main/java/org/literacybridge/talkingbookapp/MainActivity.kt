package org.literacybridge.talkingbookapp

import AppNavHost
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.ui.authenticator.SignedInState
import com.amplifyframework.ui.authenticator.ui.Authenticator
import org.literacybridge.talkingbookapp.helpers.dfu.Dfu
import org.literacybridge.talkingbookapp.helpers.dfu.Usb
import org.literacybridge.talkingbookapp.ui.theme.TalkingBookAppTheme


const val TAG = "TalkingBook";

class MainActivity : ComponentActivity(), Handler.Callback, Usb.OnUsbChangeListener,
    Dfu.DfuListener {

    private lateinit var usb: Usb
    private lateinit var dfu: Dfu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Amplify.addPlugin(AWSCognitoAuthPlugin())
        Amplify.configure(applicationContext)

        // Setup dfu
        dfu = Dfu(Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID)
        dfu.setListener(this)

//        val navController = rememberNavController()
//        NavHost(navController = navController, startDestination = "profile") {
//            composable("profile") { Profile(/*...*/) }
//            composable("friendslist") { FriendsList(/*...*/) }
//            /*...*/
//        }

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

        Amplify.Auth.getCurrentUser({
            Log.i(TAG, "session = $it")
        }, {
            Log.e(TAG, "auth error = $it")
        })

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

        Amplify.Auth.fetchAuthSession(
            { it ->
                Log.i(TAG, it.isSignedIn.toString())
            },
//            { Log.i("AmplifyQuickstart", "Auth session = $it") },
            { error -> Log.e("AmplifyQuickstart", "Failed to fetch auth session", error) }
        )
    }


    override fun onStart() {
        super.onStart()

        /* Setup USB */usb = Usb(this)
        usb.setUsbManager(getSystemService(USB_SERVICE) as UsbManager)
        usb.setOnUsbChangeListener(this)

        // Handle two types of intents. Device attachment and permission
        registerReceiver(usb.getmUsbReceiver(), IntentFilter(Usb.ACTION_USB_PERMISSION))
        registerReceiver(usb.getmUsbReceiver(), IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED))
        registerReceiver(usb.getmUsbReceiver(), IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED))


        // Handle case where USB device is connected before app launches;
        // hence ACTION_USB_DEVICE_ATTACHED will not occur so we explicitly call for permission
        usb.requestPermission(this, Usb.USB_VENDOR_ID, Usb.USB_PRODUCT_ID)
    }

    override fun onStop() {
        super.onStop()

        /* USB */dfu.setUsb(null)
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
        val deviceInfo = usb.getDeviceInfo(usb.usbDevice)
        Log.d(TAG, "$deviceInfo")
//        status.setText(deviceInfo)
        dfu.setUsb(usb)
    }
}
