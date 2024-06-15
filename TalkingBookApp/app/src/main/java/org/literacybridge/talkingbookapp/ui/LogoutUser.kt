package org.literacybridge.talkingbookapp.ui

import Screen
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.core.Amplify
import org.literacybridge.talkingbookapp.view_models.UserViewModel


@Composable
fun LogoutUser(
    navController: NavController
) {
    Log.d("Logout-user", "User is being logged out")
    Amplify.Auth.signOut { signOutResult ->
        when(signOutResult) {
            is AWSCognitoAuthSignOutResult.CompleteSignOut -> {
                // Sign Out completed fully and without errors.
                Log.i("Logout-user", "Signed out successfully")
               navController.navigate(Screen.LOGIN.name)
            }
            is AWSCognitoAuthSignOutResult.PartialSignOut -> {
                // Sign Out completed with some errors. User is signed out of the device.
                signOutResult.hostedUIError?.let {
                    Log.e("Logout-user", "HostedUI Error", it.exception)
                    // Optional: Re-launch it.url in a Custom tab to clear Cognito web session.

                }
                signOutResult.globalSignOutError?.let {
                    Log.e("Logout-user", "GlobalSignOut Error", it.exception)
                    // Optional: Use escape hatch to retry revocation of it.accessToken.
                }
                signOutResult.revokeTokenError?.let {
                    Log.e("Logout-user", "RevokeToken Error", it.exception)
                    // Optional: Use escape hatch to retry revocation of it.refreshToken.
                }
            }
            is AWSCognitoAuthSignOutResult.FailedSignOut -> {
                // Sign Out failed with an exception, leaving the user signed in.
                Log.e("Logout-user", "Sign out Failed", signOutResult.exception)
            }
        }
    }
}
