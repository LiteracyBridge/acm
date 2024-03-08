import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.core.Amplify
import com.amplifyframework.ui.authenticator.ui.Authenticator
import org.literacybridge.talkingbookapp.R
import org.literacybridge.talkingbookapp.view_models.UserViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: UserViewModel = viewModel()
) {
    viewModel.test();

    return Authenticator(
        headerContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.amplio_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.Center)
                )
            }
        },
//        footerContent = {
//        Text(
//            "© All Rights Reserved", modifier = Modifier.align(Alignment.CenterHorizontally)
//        )
//    }
    )
    { state ->
        Amplify.Auth.fetchAuthSession(
            { result ->
                val cognitoAuthSession = result as AWSCognitoAuthSession
                val accessToken = cognitoAuthSession.accessToken
                viewModel.setToken(accessToken!!)

                // Use the access token for your API calls or other authorized actions
                Log.i("AuthToken", "Access token: $accessToken")

//                TODO; save to datastore
            },
            { error ->
                // Handle any errors that might occur during token fetching
                Log.e("AuthError", "Failed to fetch access token: $error")
            }
        )
        navController.navigate(Screen.HOME.name);
//        SignedInContent(state)
    }
    // Retrieve data from next screen
//    val msg =
//        navController.currentBackStackEntry?.savedStateHandle?.get<String>("msg")
//    Column(
//        Modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Button(onClick = { navController.navigate("secondscreen") }) {
//            Text("Go to next screen")
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//        msg?.let {
//            Text(it)
//        }
//    }
}