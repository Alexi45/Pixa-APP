package com.riscart.pixa.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riscart.pixa.juego.formatearTiempo

/** Los tres tamaños jugables. */
data class Dificultad(val tamano: Int, val nombre: String, val descripcion: String)

val DIFICULTADES = listOf(
    Dificultad(5, "Fácil", "5 × 5 · un café"),
    Dificultad(10, "Normal", "10 × 10 · un rato"),
    Dificultad(15, "Difícil", "15 × 15 · para tomárselo en serio"),
)

@Composable
fun PantallaInicio(
    completados: Int,
    racha: Int,
    pistas: Int,
    diarioHecho: Boolean,
    mejorTiempo: (Int) -> Int?,
    onJugarDiario: () -> Unit,
    onJugar: (Dificultad) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = coloresTablero()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(Modifier.padding(horizontal = 22.dp)) {
            Spacer(Modifier.height(40.dp))
            Text("Pixa", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                "Nonogramas: coloca las piezas siguiendo los números",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }

        Cenefa(colores)

        Column(Modifier.padding(horizontal = 22.dp)) {
            Spacer(Modifier.height(22.dp))

            TarjetaDiaria(racha = racha, hecho = diarioHecho, onJugar = onJugarDiario)

            Spacer(Modifier.height(24.dp))

            Text("Jugar", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))

            DIFICULTADES.forEach { dificultad ->
                TarjetaDificultad(
                    dificultad = dificultad,
                    mejor = mejorTiempo(dificultad.tamano),
                    colores = colores,
                    onClick = { onJugar(dificultad) },
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Estadistica("Resueltos", completados.toString(), Modifier.weight(1f))
                Estadistica("Racha", if (racha > 0) "$racha días" else "—", Modifier.weight(1f))
                Estadistica("Pistas", pistas.toString(), Modifier.weight(1f))
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

/** La tira de olambrillas que remata la cabecera. */
@Composable
private fun Cenefa(colores: ColoresTablero) {
    val primario = MaterialTheme.colorScheme.primary
    val secundario = MaterialTheme.colorScheme.secondary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
    ) {
        dibujarCenefa(junta = colores.junta, primero = primario, segundo = secundario)
    }
}

@Composable
private fun TarjetaDiaria(racha: Int, hecho: Boolean, onJugar: () -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            // Sin destello: en una pieza tan grande el reflejo se estira y
            // parece un botón de cristal de 2008, no cerámica.
            .fondoDePieza(
                color = MaterialTheme.colorScheme.primary,
                radio = 20.dp,
                relieve = 0.85f,
            )
            .clickable(onClick = onJugar)
            .padding(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Puzzle del día",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = when {
                    hecho && racha > 1 -> "Hecho ✓ · llevas $racha días seguidos"
                    hecho -> "Hecho ✓ · vuelve mañana"
                    racha > 0 -> "Mantén tu racha de $racha días"
                    else -> "Uno nuevo cada día, igual para todos"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f),
            )
        }
        Text(
            text = if (hecho) "✓" else "▶",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 24.sp,
        )
    }
}

@Composable
private fun TarjetaDificultad(
    dificultad: Dificultad,
    mejor: Int?,
    colores: ColoresTablero,
    onClick: () -> Unit,
) {
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .fondoDePieza(color = MaterialTheme.colorScheme.surface, radio = 16.dp, relieve = 0.3f)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniPanel(dificultad.tamano, colores)
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(dificultad.nombre, style = MaterialTheme.typography.titleMedium)
            Text(
                dificultad.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (mejor != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Mejor",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatearTiempo(mejor),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Un panelito de muestra que insinúa el tamaño de la dificultad. */
@Composable
private fun MiniPanel(tamano: Int, colores: ColoresTablero) {
    val lado = when (tamano) {
        5 -> 2
        10 -> 3
        else -> 4
    }
    val patron = remember(lado) {
        val semilla = lado * 37
        List(lado * lado) { (it * semilla + it / lado) % 3 != 0 }
    }
    Canvas(Modifier.size(44.dp)) {
        val junta = size.width * 0.055f
        val paso = (size.width - junta) / lado
        val radio = paso * 0.16f
        drawRoundRect(
            color = colores.junta,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radio * 2f, radio * 2f),
        )
        for (fila in 0 until lado) {
            for (columna in 0 until lado) {
                val puesta = patron[fila * lado + columna]
                dibujarPieza(
                    x = junta / 2f + columna * paso,
                    y = junta / 2f + fila * paso,
                    lado = paso - junta,
                    color = if (puesta) colores.piezaPuesta else colores.piezaVacia,
                    radio = radio,
                    destello = false,
                    relieve = if (puesta) 1f else 0.5f,
                )
            }
        }
    }
}

@Composable
private fun Estadistica(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .fondoDePieza(color = MaterialTheme.colorScheme.surface, radio = 14.dp, relieve = 0.3f)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(valor, style = MaterialTheme.typography.headlineMedium)
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
