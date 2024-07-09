import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.literacybridge.androidtbloader.ui.CollectStatisticsScreen
import org.literacybridge.androidtbloader.ui.ContentDownloaderScreen
import org.literacybridge.androidtbloader.ui.LogoutScreen
import org.literacybridge.androidtbloader.ui.ProgramSelectionScreen
import org.literacybridge.androidtbloader.ui.talkingbook_update.ContentUpdateScreen
import org.literacybridge.androidtbloader.ui.talkingbook_update.ContentVariantScreen
import org.literacybridge.androidtbloader.ui.talkingbook_update.ContentVerificationScreen
import org.literacybridge.androidtbloader.ui.talkingbook_update.RecipientScreen
import org.literacybridge.androidtbloader.ui.upload_status.UploadStatusScreen

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
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                LoginScreen(navController)
            }
        }
        composable(NavigationItem.Home.route) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                HomeScreen(navController)
            }
        }
        composable(NavigationItem.ProgramSelection.route) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                ProgramSelectionScreen(navController)
            }
        }
        composable(NavigationItem.CollectData.route) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                CollectStatisticsScreen(navController)
            }
        }
        composable(NavigationItem.ContentDownloader.route) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                ContentDownloaderScreen(navController)
            }
        }
        composable(NavigationItem.Recipient.route) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                RecipientScreen(navController)
            }
        }
        composable(NavigationItem.ContentVerification.route) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                ContentVerificationScreen(navController)
            }
        }
        composable(NavigationItem.ContentUpdate.route) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                ContentUpdateScreen(navController)
            }
        }
        composable(NavigationItem.ContentVariant.route) {
            CompositionLocalProvider(
                value = LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                ContentVariantScreen(navController = navController)
            }
        }
        composable(NavigationItem.UploadStatus.route) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                UploadStatusScreen(navController)
            }
        }
        composable(NavigationItem.FirmwareUpdate.route) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                FirmwareUpdateScreen(navController)
            }
        }
        composable(NavigationItem.Logout.route) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ){
                LogoutScreen(navController)
            }
        }
    }
}