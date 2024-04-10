enum class Screen {
    HOME,
    LOGIN,
    PROGRAM_SELECTION,
    COLLECT_DATA,
    CONTENT_DOWNLOADER,
    RECIPIENT
}

sealed class NavigationItem(val route: String) {
    object Home : NavigationItem(Screen.HOME.name)
    object Login : NavigationItem(Screen.LOGIN.name)
    object ProgramSelection : NavigationItem(Screen.PROGRAM_SELECTION.name)
    object CollectData : NavigationItem(Screen.COLLECT_DATA.name)
    object ContentDownloader : NavigationItem(Screen.CONTENT_DOWNLOADER.name)
    object Recipient : NavigationItem(Screen.RECIPIENT.name)

}