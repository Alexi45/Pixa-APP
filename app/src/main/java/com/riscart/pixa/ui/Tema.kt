package com.riscart.pixa.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Paleta propia en vez de la de Material por defecto: un verde azulado profundo
 * sobre papel cálido. El modo oscuro no es el claro invertido — cada color tiene
 * su valor pensado para fondo oscuro.
 */
private val VerdeProfundo = Color(0xFF1E4D45)
private val VerdeMedio = Color(0xFF2E6B5F)
private val VerdeClaro = Color(0xFF7FCBBA)
private val Terracota = Color(0xFFD1704F)
private val PapelClaro = Color(0xFFF6F4EF)
private val PapelOscuro = Color(0xFF11161A)

private val EsquemaClaro = lightColorScheme(
    primary = VerdeProfundo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8E2),
    onPrimaryContainer = Color(0xFF0C2E29),
    secondary = Terracota,
    onSecondary = Color.White,
    background = PapelClaro,
    onBackground = Color(0xFF1B1F1E),
    surface = Color.White,
    onSurface = Color(0xFF1B1F1E),
    surfaceVariant = Color(0xFFE8E5DD),
    onSurfaceVariant = Color(0xFF5A5F5C),
    outline = Color(0xFFCFCAC0),
    error = Color(0xFFB3261E),
)

private val EsquemaOscuro = darkColorScheme(
    primary = VerdeClaro,
    onPrimary = Color(0xFF07211C),
    primaryContainer = Color(0xFF1E4D45),
    onPrimaryContainer = Color(0xFFBFE8DD),
    secondary = Color(0xFFE79374),
    onSecondary = Color(0xFF3A1507),
    background = PapelOscuro,
    onBackground = Color(0xFFE6E3DC),
    surface = Color(0xFF1A2026),
    onSurface = Color(0xFFE6E3DC),
    surfaceVariant = Color(0xFF262D33),
    onSurfaceVariant = Color(0xFF9AA09D),
    outline = Color(0xFF3A4349),
    error = Color(0xFFF2B8B5),
)

/** Colores propios del tablero, que no encajan en el esquema de Material. */
data class ColoresTablero(
    val celdaVacia: Color,
    val celdaPintada: Color,
    val celdaTachada: Color,
    val lineaFina: Color,
    val lineaGruesa: Color,
    val resaltado: Color,
    val textoPista: Color,
)

val coloresTableroClaro = ColoresTablero(
    celdaVacia = Color(0xFFFFFFFF),
    celdaPintada = VerdeProfundo,
    celdaTachada = Color(0xFFB9B4A9),
    lineaFina = Color(0xFFDDD8CE),
    lineaGruesa = Color(0xFF8C9794),
    resaltado = Color(0x14000000),
    textoPista = Color(0xFF4A524F),
)

val coloresTableroOscuro = ColoresTablero(
    celdaVacia = Color(0xFF1E252B),
    celdaPintada = VerdeClaro,
    celdaTachada = Color(0xFF5B6469),
    lineaFina = Color(0xFF2C343A),
    lineaGruesa = Color(0xFF55605F),
    resaltado = Color(0x18FFFFFF),
    textoPista = Color(0xFFA8B2AF),
)

private val tipografia = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 21.sp,
    ),
)

@Composable
fun TemaPixa(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val esquema = if (oscuro) EsquemaOscuro else EsquemaClaro
    val vista = LocalView.current

    if (!vista.isInEditMode) {
        val contexto = LocalContext.current
        SideEffect {
            val ventana = (contexto as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(ventana, vista).isAppearanceLightStatusBars = !oscuro
        }
    }

    MaterialTheme(colorScheme = esquema, typography = tipografia, content = content)
}

/** Los colores del tablero que tocan según el tema activo. */
@Composable
fun coloresTablero(oscuro: Boolean = isSystemInDarkTheme()): ColoresTablero =
    if (oscuro) coloresTableroOscuro else coloresTableroClaro
