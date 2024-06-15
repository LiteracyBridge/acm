import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
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
        // TODO: fix rare bug in amplify auth where the user is not able to login after not using the app for
        // some weeks
        // Encountered AuthException: SignedInException{message=There is already a user signed in., cause=null,
        // recoverySuggestion=Sign out the user first before signing in again.}

        LaunchedEffect("login") {
            Amplify.Auth.fetchAuthSession(
                { result ->
                    val cognitoAuthSession = result as AWSCognitoAuthSession
                    val accessToken = cognitoAuthSession.tokensResult.value?.idToken

                    viewModel.setToken(accessToken!!, result.userSubResult.value!!, navController)

                },
                { error ->
                    // Handle any errors that might occur during token fetching
                    Log.e("AuthError", "Failed to fetch access token: $error")
                }
            )
        }

        // Show a loading indicator while fetching user details
        if (viewModel.isLoading.value) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.width(100.dp),
                )
            }
        }
    }
}
