package com.riscart.pixa.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(44.dp))

        Text("Pixa", style = MaterialTheme.typography.displaySmall)
        Text(
            "Nonogramas: pinta siguiendo los números",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )

        Spacer(Modifier.height(26.dp))

        TarjetaDiaria(racha = racha, hecho = diarioHecho, onJugar = onJugarDiario)

        Spacer(Modifier.height(26.dp))

        Text(
            "Jugar",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))

        DIFICULTADES.forEach { dificultad ->
            TarjetaDificultad(
                dificultad = dificultad,
                mejor = mejorTiempo(dificultad.tamano),
                onClick = { onJugar(dificultad) },
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Estadistica("Resueltos", completados.toString(), Modifier.weight(1f))
            Estadistica("Racha", if (racha > 0) "$racha días" else "—", Modifier.weight(1f))
            Estadistica("Pistas", pistas.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun TarjetaDiaria(racha: Int, hecho: Boolean, onJugar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary,
        onClick = onJugar,
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Puzzle del día",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        hecho && racha > 1 -> "Hecho ✓ · llevas $racha días seguidos"
                        hecho -> "Hecho ✓ · vuelve mañana"
                        racha > 0 -> "Mantén tu racha de $racha días"
                        else -> "Uno nuevo cada día, igual para todos"
                    },
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                    fontSize = 13.sp,
                )
            }
            Text(
                text = if (hecho) "✓" else "▶",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 26.sp,
            )
        }
    }
}

@Composable
private fun TarjetaDificultad(dificultad: Dificultad, mejor: Int?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniRejilla(dificultad.tamano)
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    dificultad.nombre,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    dificultad.descripcion,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (mejor != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Mejor",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatearTiempo(mejor),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Un adorno: una rejillita que insinúa el tamaño de la dificultad. */
@Composable
private fun MiniRejilla(tamano: Int) {
    val lado = when (tamano) {
        5 -> 2
        10 -> 3
        else -> 4
    }
    val patron = remember(lado) {
        val semilla = lado * 37
        List(lado * lado) { (it * semilla + it / lado) % 3 != 0 }
    }
    Column(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(lado) { fila ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(lado) { columna ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                if (patron[fila * lado + columna]) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Estadistica(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            valor,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            etiqueta,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
