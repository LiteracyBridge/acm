enum class Screen {
    HOME,
    LOGIN,
    PROGRAM_SELECTION,
    COLLECT_DATA,
    CONTENT_DOWNLOADER,
    RECIPIENT,
    CONTENT_VERIFICATION,
    CONTENT_UPDATE,
    UPLOAD_STATUS,
    FIRMWARE_REFRESH,
}

sealed class NavigationItem(val route: String) {
    object Home : NavigationItem(Screen.HOME.name)
    object Login : NavigationItem(Screen.LOGIN.name)
    object ProgramSelection : NavigationItem(Screen.PROGRAM_SELECTION.name)
    object CollectData : NavigationItem(Screen.COLLECT_DATA.name)
    object ContentDownloader : NavigationItem(Screen.CONTENT_DOWNLOADER.name)
    object Recipient : NavigationItem(Screen.RECIPIENT.name)
    object ContentVerification : NavigationItem(Screen.CONTENT_VERIFICATION.name)
    object ContentUpdate : NavigationItem(Screen.CONTENT_UPDATE.name)
    object UploadStatus : NavigationItem(Screen.UPLOAD_STATUS.name)
    object FirmwareUpdate : NavigationItem(Screen.FIRMWARE_REFRESH.name)

}