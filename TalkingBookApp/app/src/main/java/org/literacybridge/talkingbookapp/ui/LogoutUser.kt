package org.literacybridge.talkingbookapp.ui

import Screen
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.core.Amplify
import io.sentry.Sentry.captureException


@Composable
fun LogoutUser(
    navController: NavController
) {
    Log.d("Logout-user", "User is being logged out")
    Amplify.Auth.signOut { signOutResult ->
        when (signOutResult) {
            is AWSCognitoAuthSignOutResult.CompleteSignOut -> {
                // Sign Out completed fully and without errors.
                Log.i("Logout-user", "Signed out successfully")
                navController.navigate(Screen.LOGIN.name)
            }

            is AWSCognitoAuthSignOutResult.PartialSignOut -> {
                // Sign Out completed with some errors. User is signed out of the device.
                signOutResult.hostedUIError?.let {
                    // Optional: Re-launch it.url in a Custom tab to clear Cognito web session.
                    Log.e("Logout-user", "HostedUI Error", it.exception)
                    captureException(it.exception)
                }
                signOutResult.globalSignOutError?.let {
                    // Optional: Use escape hatch to retry revocation of it.accessToken.
                    Log.e("Logout-user", "GlobalSignOut Error", it.exception)
                    captureException(it.exception)
                }
                signOutResult.revokeTokenError?.let {
                    // Optional: Use escape hatch to retry revocation of it.refreshToken.
                    Log.e("Logout-user", "RevokeToken Error", it.exception)
                    captureException(it.exception)
                }
            }

            is AWSCognitoAuthSignOutResult.FailedSignOut -> {
                // Sign Out failed with an exception, leaving the user signed in.
                Log.e("Logout-user", "Sign out Failed", signOutResult.exception)
                captureException(signOutResult.exception)
            }
        }
    }
}
