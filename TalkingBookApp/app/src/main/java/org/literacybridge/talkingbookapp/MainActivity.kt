package org.literacybridge.talkingbookapp

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
import org.literacybridge.talkingbookapp.ui.theme.TalkingBookAppTheme
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.ui.authenticator.SignInState
import com.amplifyframework.ui.authenticator.SignedInState
import com.amplifyframework.ui.authenticator.rememberAuthenticatorState
import com.amplifyframework.ui.authenticator.ui.Authenticator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Amplify.addPlugin(AWSCognitoAuthPlugin())
        Amplify.configure(applicationContext)

        setContent {
            TalkingBookAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                    Authenticator { state ->
                        SignedInContent(state)
                    }
                }
            }
        }

        Amplify.Auth.fetchAuthSession(
            { Log.i("AmplifyQuickstart", "Auth session = $it") },
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
                    modifier=Modifier.width(40.dp)
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
        // ...
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