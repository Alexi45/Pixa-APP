package com.riscart.pixa.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * La pieza vidriada: el gesto gráfico del que sale todo lo demás.
 *
 * No es un rectángulo de color. Es cerámica esmaltada: el vidriado va de claro
 * arriba a oscuro abajo, la luz entra por la esquina de arriba a la izquierda y
 * el borde de abajo a la derecha se hunde en la junta. Tres capas baratas que
 * cambian por completo la sensación al mirar el tablero.
 */
fun DrawScope.dibujarPieza(
    x: Float,
    y: Float,
    lado: Float,
    color: Color,
    radio: Float = lado * 0.12f,
    destello: Boolean = true,
    /** Cuánto relieve tiene el esmalte. Las piezas claras lo llevan suave: con
     *  el mismo que las de color parecían nubes hinchadas, no cerámica. */
    relieve: Float = 1f,
) = dibujarPieza(x, y, lado, lado, color, radio, destello, relieve)

/** La misma pieza, pero rectangular: tarjetas, botones y paneles. */
fun DrawScope.dibujarPieza(
    x: Float,
    y: Float,
    ancho: Float,
    alto: Float,
    color: Color,
    radio: Float,
    destello: Boolean = true,
    relieve: Float = 1f,
) {
    val lado = minOf(ancho, alto)
    val esquina = CornerRadius(radio, radio)
    val arriba = Offset(x, y)
    val tamano = Size(ancho, alto)

    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to aclarar(color, 0.11f * relieve),
            0.55f to color,
            1f to oscurecer(color, 0.10f * relieve),
            startY = y,
            endY = y + alto,
        ),
        topLeft = arriba,
        size = tamano,
        cornerRadius = esquina,
    )

    drawRoundRect(
        brush = Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.26f * relieve),
            0.62f to Color.Transparent,
            start = Offset(x, y),
            end = Offset(x + ancho * 0.9f, y + alto * 0.9f),
        ),
        topLeft = arriba,
        size = tamano,
        cornerRadius = esquina,
    )

    drawRoundRect(
        brush = Brush.linearGradient(
            0.45f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.16f * relieve),
            start = Offset(x, y),
            end = Offset(x + ancho, y + alto),
        ),
        topLeft = arriba,
        size = tamano,
        cornerRadius = esquina,
    )

    // El reflejo de la luz en el esmalte. Va con los extremos difuminados: con
    // los bordes duros no parecía un brillo, parecía un guion pintado encima.
    // Y solo en piezas grandes; en un 15 × 15 sería suciedad.
    if (destello && lado >= 64f) {
        val brilloAncho = ancho * 0.62f
        val brilloAlto = lado * 0.055f
        val izquierda = x + ancho * 0.19f
        val arriba2 = y + alto * 0.11f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.5f to Color.White.copy(alpha = 0.16f),
                1f to Color.Transparent,
                startX = izquierda,
                endX = izquierda + brilloAncho,
            ),
            topLeft = Offset(izquierda, arriba2),
            size = Size(brilloAncho, brilloAlto),
            cornerRadius = CornerRadius(brilloAlto / 2f, brilloAlto / 2f),
        )
    }
}

/** La marca de "aquí no va pieza": dos trazos con las puntas redondeadas. */
fun DrawScope.dibujarCruz(x: Float, y: Float, lado: Float, color: Color) {
    val margen = lado * 0.33f
    val grosor = (lado * 0.085f).coerceAtLeast(2f)
    drawLine(
        color = color,
        start = Offset(x + margen, y + margen),
        end = Offset(x + lado - margen, y + lado - margen),
        strokeWidth = grosor,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(x + lado - margen, y + margen),
        end = Offset(x + margen, y + lado - margen),
        strokeWidth = grosor,
        cap = StrokeCap.Round,
    )
}

/**
 * Cuece la pieza una sola vez en un bitmap para luego estamparla.
 *
 * Un 15 × 15 son 225 piezas y cada una lleva tres degradados. Dibujarlas de
 * verdad en cada fotograma mientras arrastras el dedo sería tirar el
 * rendimiento por la ventana: se dibujan una vez por tamaño y color, y a partir
 * de ahí el tablero solo estampa imágenes.
 */
fun cocerPieza(
    paso: Float,
    junta: Float,
    color: Color,
    radio: Float,
    destello: Boolean,
    cruz: Color? = null,
    relieve: Float = 1f,
): ImageBitmap {
    val lienzo = ceil(paso).toInt().coerceAtLeast(2)
    val lado = paso - junta
    val imagen = ImageBitmap(lienzo, lienzo)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(imagen),
        size = Size(lienzo.toFloat(), lienzo.toFloat()),
    ) {
        val desplazamiento = junta / 2f
        dibujarPieza(desplazamiento, desplazamiento, lado, color, radio, destello, relieve)
        if (cruz != null) dibujarCruz(desplazamiento, desplazamiento, lado, cruz)
    }
    return imagen
}

fun aclarar(color: Color, cantidad: Float): Color = lerp(color, Color.White, cantidad)

fun oscurecer(color: Color, cantidad: Float): Color = lerp(color, Color.Black, cantidad)

/** Pone una pieza vidriada de fondo en cualquier composable: tarjetas, botones… */
fun Modifier.fondoDePieza(
    color: Color,
    radio: Dp = 14.dp,
    destello: Boolean = false,
    relieve: Float = 0.55f,
): Modifier = drawBehind {
    dibujarPieza(0f, 0f, size.width, size.height, color, radio.toPx(), destello, relieve)
}

/**
 * Una cenefa: la tira de olambrillas (los rombos pequeños) con la que se remata
 * un panel de azulejos. Es puro adorno, y es justo lo que hace que la pantalla
 * de inicio no parezca una lista de tarjetas más.
 */
fun DrawScope.dibujarCenefa(
    junta: Color,
    primero: Color,
    segundo: Color,
) {
    val alto = size.height
    drawRect(color = junta, size = Size(size.width, alto))

    // Los listeles de arriba y abajo: sin ellos la cenefa parece una pegatina.
    val listel = (alto * 0.06f).coerceAtLeast(1.5f)
    drawRect(color = oscurecer(junta, 0.18f), size = Size(size.width, listel))
    drawRect(
        color = oscurecer(junta, 0.18f),
        topLeft = Offset(0f, alto - listel),
        size = Size(size.width, listel),
    )

    val lado = alto * 0.42f
    val paso = alto * 0.86f
    var centro = paso / 2f
    var i = 0
    while (centro - lado < size.width) {
        rotate(degrees = 45f, pivot = Offset(centro, alto / 2f)) {
            drawRect(
                color = if (i % 2 == 0) primero else segundo,
                topLeft = Offset(centro - lado / 2f, alto / 2f - lado / 2f),
                size = Size(lado, lado),
            )
        }
        centro += paso
        i++
    }
}
