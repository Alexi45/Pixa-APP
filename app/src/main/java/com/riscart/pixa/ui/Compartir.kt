package com.riscart.pixa.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.riscart.pixa.R
import com.riscart.pixa.engine.Puzzle
import com.riscart.pixa.juego.formatearTiempo
import java.io.File

/**
 * Compartir el panel terminado.
 *
 * Es la mejor publicidad que puede tener un juego así y no cuesta un céntimo:
 * la gente enseña el dibujo que le ha salido. Se pinta una imagen limpia (sin
 * interfaz, sin marcas de agua raras) con el dibujo, su nombre y el tiempo.
 */
object Compartir {

    private const val LADO = 1080
    private const val MARGEN = 72f

    fun panel(contexto: Context, puzzle: Puzzle, segundos: Int, errores: Int) {
        val imagen = pintar(contexto, puzzle, segundos, errores)
        val carpeta = File(contexto.cacheDir, "compartir").apply { mkdirs() }
        val archivo = File(carpeta, "pixa.png")
        archivo.outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            archivo,
        )

        val texto = buildString {
            append(puzzle.nombre?.let { "$it · " } ?: "")
            append("${puzzle.width} × ${puzzle.height} en ${formatearTiempo(segundos)}")
            if (errores == 0) append(" sin fallos")
            append(" · Pixa")
        }

        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, texto)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        contexto.startActivity(
            Intent.createChooser(envio, "Compartir el panel").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    private fun pintar(contexto: Context, puzzle: Puzzle, segundos: Int, errores: Int): Bitmap {
        val altoTexto = 190
        val imagen = createBitmap(LADO, LADO + altoTexto)
        val lienzo = Canvas(imagen)
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG)

        pincel.color = CAL
        lienzo.drawRect(0f, 0f, LADO.toFloat(), (LADO + altoTexto).toFloat(), pincel)

        val disponible = LADO - MARGEN * 2
        val paso = disponible / puzzle.width
        val junta = (paso * 0.075f).coerceIn(2f, 10f)
        val radio = (paso * 0.11f).coerceIn(2f, 10f)

        // El panel de junta sobre el que se asientan las piezas.
        pincel.shader = null
        pincel.color = JUNTA
        lienzo.drawRoundRect(
            RectF(
                MARGEN - junta / 2f,
                MARGEN - junta / 2f,
                MARGEN + puzzle.width * paso + junta / 2f,
                MARGEN + puzzle.height * paso + junta / 2f,
            ),
            radio * 1.8f, radio * 1.8f, pincel,
        )

        for (y in 0 until puzzle.height) {
            for (x in 0 until puzzle.width) {
                val puesta = puzzle.filledAt(x, y)
                val izquierda = MARGEN + x * paso + junta / 2f
                val arriba = MARGEN + y * paso + junta / 2f
                val lado = paso - junta
                val base = if (puesta) COBALTO else BLANCO
                pincel.shader = LinearGradient(
                    izquierda, arriba, izquierda, arriba + lado,
                    aclarar(base, if (puesta) 0.11f else 0.05f),
                    oscurecer(base, if (puesta) 0.10f else 0.05f),
                    Shader.TileMode.CLAMP,
                )
                lienzo.drawRoundRect(
                    RectF(izquierda, arriba, izquierda + lado, arriba + lado),
                    radio, radio, pincel,
                )
            }
        }
        pincel.shader = null

        val titular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TINTA
            textSize = 68f
            typeface = ResourcesCompat.getFont(contexto, R.font.bricolage_grotesque)
        }
        val detalle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GRIS
            textSize = 46f
            typeface = ResourcesCompat.getFont(contexto, R.font.plex_condensed_medium)
        }

        val base = LADO + 86f
        lienzo.drawText(puzzle.nombre ?: "Pixa", MARGEN, base, titular)
        val pie = buildString {
            append("${puzzle.width} × ${puzzle.height} · ${formatearTiempo(segundos)}")
            if (errores == 0) append(" · sin fallos")
        }
        lienzo.drawText(pie, MARGEN, base + 58f, detalle)

        return imagen
    }

    private fun createBitmap(ancho: Int, alto: Int): Bitmap =
        Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)

    private fun aclarar(color: Int, cantidad: Float): Int = mezclar(color, Color.WHITE, cantidad)

    private fun oscurecer(color: Int, cantidad: Float): Int = mezclar(color, Color.BLACK, cantidad)

    private fun mezclar(a: Int, b: Int, t: Float): Int = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt(),
    )

    private val COBALTO = Color.parseColor("#1B4F8F")
    private val BLANCO = Color.parseColor("#F8F3E9")
    private val JUNTA = Color.parseColor("#C9BFAE")
    private val CAL = Color.parseColor("#EDE7DB")
    private val TINTA = Color.parseColor("#23282E")
    private val GRIS = Color.parseColor("#5C564B")
}
