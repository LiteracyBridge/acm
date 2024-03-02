import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.amplifyframework.ui.authenticator.ui.Authenticator
import org.literacybridge.talkingbookapp.R

@Composable
fun LoginScreen(navController: NavController) {
    return Authenticator(
        headerContent = {
            Box(
//                                modifier = Modifier.size(80.dp).align(Alignment.CenterHorizontally)
            ) {
                Image(
                    painter = painterResource(R.drawable.amplio_logo),
                    contentDescription = null,
                )
            }
        },
//                        footerContent = {
//                            Text(
//                                "© All Rights Reserved",
//                                modifier = Modifier.align(Alignment.CenterHorizontally)
//                            )
//                        }
    ) { state ->
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