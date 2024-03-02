package org.literacybridge.talkingbookapp

import AppNavHost
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.ui.authenticator.SignedInState
import com.amplifyframework.ui.authenticator.ui.Authenticator
import org.literacybridge.talkingbookapp.ui.theme.TalkingBookAppTheme


const val TAG = "TalkingBook";

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Amplify.addPlugin(AWSCognitoAuthPlugin())
        Amplify.configure(applicationContext)

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
}

@Composable
fun SignedInContent(state: SignedInState) {
    Authenticator(
        headerContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
//                    .align(Alignment.CenterHorizontally)
            ) {
                Image(
                    painter = painterResource(R.drawable.amplio_logo),
                    contentDescription = null,
                    modifier = Modifier.width(40.dp)
                )
            }
        },
        footerContent = {
            Text(
                "© All Rights Reserved",
//                modifier = Modifier.align(Alignment.Center)
            )
        }
    ) {
        Text(text = "working me")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TalkingBookAppTheme {
        Greeting("Android")
    }
}