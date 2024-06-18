package org.literacybridge.talkingbookapp.ui

import Screen
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.core.Amplify
import io.sentry.Sentry.captureException


@Composable
fun LogoutUser(
    navController: NavController
) {
    val isSuccessful = remember { mutableStateOf(false) }

    if (isSuccessful.value) {
        navController.navigate(Screen.LOGIN.name){
            popUpTo(0)
        }
    }

    LaunchedEffect("logout-user") {
        Amplify.Auth.signOut { signOutResult ->
            when (signOutResult) {
                is AWSCognitoAuthSignOutResult.CompleteSignOut -> {
                    // Sign Out completed fully and without errors.
                    Log.i("Logout-user", "Signed out successfully")
                    isSuccessful.value = true
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

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.width(100.dp),
        )
    }
}
