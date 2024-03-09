//import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.literacybridge.talkingbookapp.ui.ProgramSelectionScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = NavigationItem.Login.route,
    // other parameters
) {
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavigationItem.Login.route) {
            LoginScreen(navController)
        }
        composable(NavigationItem.Home.route) {
//            val viewModel = hiltViewModel<TalkingBookViewModel>()

//            CompositionLocalProvider(
//                LocalViewModelStoreOwner provides viewModelStoreOwner
//            ) {
            HomeScreen(navController)
//            }
        }
        composable(NavigationItem.ProgramSelection.route) {
            ProgramSelectionScreen(navController)
        }
    }
}