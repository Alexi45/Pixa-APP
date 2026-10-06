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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.riscart.pixa.R

/* ─────────────────────────────────────────────────────────────────────────────
 * Azulejo.
 *
 * La idea: el tablero no es una rejilla, es un panel de azulejos, y jugar es ir
 * colocando piezas vidriadas sobre la junta. De ahí sale todo lo demás — el
 * cobalto de la pieza puesta, el blanco roto de la vacía, el beige de la junta
 * y el albero de los acentos. Nada de esto es la paleta de Material por defecto
 * ni un gris azulado de plantilla.
 * ───────────────────────────────────────────────────────────────────────────── */

private val Cobalto = Color(0xFF1B4F8F)
private val CobaltoClaro = Color(0xFF6FA8E8)
private val BlancoRoto = Color(0xFFFCFAF5)
private val Cal = Color(0xFFEDE7DB)
private val Junta = Color(0xFFC9BFAE)
private val Albero = Color(0xFFD9A441)
private val Almagre = Color(0xFFB4452E)
private val Tinta = Color(0xFF23282E)

private val NocheAzulejo = Color(0xFF0E1419)
private val NochePieza = Color(0xFF19212A)

private val EsquemaClaro = lightColorScheme(
    primary = Cobalto,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E0F0),
    onPrimaryContainer = Color(0xFF0B2444),
    secondary = Albero,
    onSecondary = Color(0xFF3A2605),
    background = Cal,
    onBackground = Tinta,
    surface = BlancoRoto,
    onSurface = Tinta,
    surfaceVariant = Color(0xFFDCD4C5),
    onSurfaceVariant = Color(0xFF5C564B),
    outline = Color(0xFFC0B6A4),
    error = Almagre,
)

private val EsquemaOscuro = darkColorScheme(
    primary = CobaltoClaro,
    onPrimary = Color(0xFF06203F),
    primaryContainer = Cobalto,
    onPrimaryContainer = Color(0xFFD3E0F0),
    secondary = Color(0xFFE8BC63),
    onSecondary = Color(0xFF2E1D04),
    background = NocheAzulejo,
    onBackground = Color(0xFFE7E2D7),
    surface = NochePieza,
    onSurface = Color(0xFFE7E2D7),
    surfaceVariant = Color(0xFF242E38),
    onSurfaceVariant = Color(0xFF9AA3AC),
    outline = Color(0xFF36414C),
    error = Color(0xFFE8826B),
)

/** Los colores del panel de azulejos, que no caben en el esquema de Material. */
data class ColoresTablero(
    val junta: Color,
    val juntaFuerte: Color,
    val piezaVacia: Color,
    val piezaPuesta: Color,
    val marcaTachada: Color,
    val bandaPistas: Color,
    val textoPista: Color,
    val textoPistaHecha: Color,
)

val coloresTableroClaro = ColoresTablero(
    junta = Junta,
    juntaFuerte = Color(0xFF9A8C72),
    piezaVacia = Color(0xFFF8F3E9),
    piezaPuesta = Cobalto,
    marcaTachada = Color(0xFFA79C88),
    bandaPistas = Color(0x0A6B5F48),
    textoPista = Color(0xFF4A4639),
    textoPistaHecha = Color(0xFFB07C1C),
)

val coloresTableroOscuro = ColoresTablero(
    // En oscuro la junta va MÁS clara que la pieza vacía: si no, el panel se
    // convierte en una mancha negra y no se ve dónde está cada casilla.
    junta = Color(0xFF2C3741),
    juntaFuerte = Color(0xFF4A5A67),
    piezaVacia = Color(0xFF151C23),
    piezaPuesta = CobaltoClaro,
    marcaTachada = Color(0xFF4E5A64),
    bandaPistas = Color(0x0AFFFFFF),
    textoPista = Color(0xFF9AA3AC),
    textoPistaHecha = Color(0xFFE8BC63),
)

/* ── Tipografía ──────────────────────────────────────────────────────────────
 * Bricolage Grotesque para los titulares (es una grotesca con sus rarezas, no
 * la Roboto del sistema) e IBM Plex Sans Condensed para todo lo demás: las
 * cifras estrechas se leen bien apiladas en las pistas, que es donde más
 * número por centímetro hay. Las dos van empaquetadas en la app (OFL), así que
 * se ve igual en cualquier móvil y sin pedir nada a la red.
 * ───────────────────────────────────────────────────────────────────────────── */

// El eje de tamaño óptico es lo que le da a Bricolage su carácter en titulares:
// más apretada y con más personalidad cuanto más grande se usa.
@OptIn(ExperimentalTextApi::class)
private fun bricolage(peso: Int, ancho: Float, optico: Float) = Font(
    resId = R.font.bricolage_grotesque,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(peso),
        FontVariation.width(ancho),
        FontVariation.opticalSizing(optico.sp),
    ),
)

private val Bricolage = FontFamily(
    bricolage(peso = 400, ancho = 100f, optico = 24f),
    bricolage(peso = 600, ancho = 100f, optico = 32f),
    bricolage(peso = 800, ancho = 96f, optico = 48f),
)

private val Plex = FontFamily(
    Font(R.font.plex_condensed_regular, FontWeight.Normal),
    Font(R.font.plex_condensed_medium, FontWeight.Medium),
    Font(R.font.plex_condensed_semibold, FontWeight.SemiBold),
)

/** Para las cifras de las pistas y los cronómetros. */
val FamiliaCifras = Plex

private val tipografia = Typography(
    displayLarge = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight(800),
        fontSize = 52.sp,
        letterSpacing = (-1.6).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight(800),
        fontSize = 34.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight(600),
        fontSize = 27.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight(600),
        fontSize = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Plex,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyLarge = TextStyle(fontFamily = Plex, fontSize = 16.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontFamily = Plex, fontSize = 14.sp, letterSpacing = 0.2.sp),
    bodySmall = TextStyle(fontFamily = Plex, fontSize = 12.sp, letterSpacing = 0.3.sp),
    labelLarge = TextStyle(
        fontFamily = Plex,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.4.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Plex,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
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

/** Los colores del panel que tocan según el tema activo. */
@Composable
fun coloresTablero(oscuro: Boolean = isSystemInDarkTheme()): ColoresTablero =
    if (oscuro) coloresTableroOscuro else coloresTableroClaro
