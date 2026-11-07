package cn.ws9.daoyi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val Scheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = GoldPale,
    onPrimaryContainer = Ink,
    secondary = Cinnabar,
    onSecondary = White,
    secondaryContainer = CinnabarPale,
    onSecondaryContainer = Cinnabar,
    tertiary = Jade,
    onTertiary = White,
    tertiaryContainer = JadePale,
    onTertiaryContainer = Jade,
    background = Paper,
    onBackground = Ink,
    surface = CardBg,
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = InkSoft,
    outline = HairLine,
    error = Cinnabar,
    onError = White
)

@Composable
fun DaoYiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = DaoYiTypography,
        content = content
    )
}
