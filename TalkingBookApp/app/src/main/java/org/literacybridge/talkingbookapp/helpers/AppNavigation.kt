enum class Screen {
    HOME,
    LOGIN,
    PROGRAM_SELECTION
}

sealed class NavigationItem(val route: String) {
    object Home : NavigationItem(Screen.HOME.name)
    object Login : NavigationItem(Screen.LOGIN.name)
    object ProgramSelection : NavigationItem(Screen.PROGRAM_SELECTION.name)
}