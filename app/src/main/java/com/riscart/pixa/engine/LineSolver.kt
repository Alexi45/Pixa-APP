package com.riscart.pixa.engine

/**
 * Deduce lo que se puede saber *con certeza* de una sola línea (fila o columna).
 *
 * La idea: en vez de probar combinaciones a lo bruto, se construye una tabla de
 * viabilidad con programación dinámica —"¿puedo colocar los bloques que quedan en
 * las casillas que quedan?"— y después se recorre hacia delante marcando, para cada
 * casilla, si existe alguna solución donde está pintada y si existe alguna donde
 * está vacía.
 *
 * Si una casilla sale pintada en *todas* las soluciones posibles, es seguro
 * pintarla. Si sale vacía en todas, es seguro tacharla. Si puede ser las dos cosas,
 * aún no se sabe. Eso es exactamente el razonamiento que hace una persona jugando,
 * y garantiza que nunca se pide adivinar.
 */
object LineSolver {

    /**
     * @param linea estado actual de la línea
     * @param pistas números de la línea (una línea vacía viene como [0])
     * @return la línea con todo lo deducible, o null si las pistas son imposibles
     */
    fun resolver(linea: List<Cell>, pistas: List<Int>): List<Cell>? {
        val n = linea.size
        val bloques = pistas.filter { it > 0 }
        val k = bloques.size

        // sufijoSinPintar[i] = en linea[i..] no hay ninguna casilla ya pintada
        val sufijoSinPintar = BooleanArray(n + 1)
        sufijoSinPintar[n] = true
        for (i in n - 1 downTo 0) {
            sufijoSinPintar[i] = sufijoSinPintar[i + 1] && linea[i] != Cell.FILLED
        }

        // viable[i][j] = ¿caben los bloques j..k-1 en linea[i..n-1]?
        val viable = Array(n + 1) { BooleanArray(k + 1) }
        for (i in 0..n) viable[i][k] = sufijoSinPintar[i]

        for (j in k - 1 downTo 0) {
            val largo = bloques[j]
            for (i in n - 1 downTo 0) {
                // Opción A: dejar esta casilla vacía
                var ok = linea[i] != Cell.FILLED && viable[i + 1][j]
                // Opción B: empezar aquí el bloque j
                if (!ok) {
                    val tras = sitioTrasBloque(linea, i, largo, n)
                    if (tras >= 0 && viable[tras][j + 1]) ok = true
                }
                viable[i][j] = ok
            }
        }

        if (!viable[0][0]) return null

        // Recorrido hacia delante: qué puede ser cada casilla en alguna solución válida.
        val puedePintada = BooleanArray(n)
        val puedeVacia = BooleanArray(n)
        val alcanzable = Array(n + 1) { BooleanArray(k + 1) }
        alcanzable[0][0] = true

        for (i in 0 until n) {
            for (j in 0..k) {
                if (!alcanzable[i][j]) continue

                if (linea[i] != Cell.FILLED && viable[i + 1][j]) {
                    puedeVacia[i] = true
                    alcanzable[i + 1][j] = true
                }

                if (j < k) {
                    val largo = bloques[j]
                    val tras = sitioTrasBloque(linea, i, largo, n)
                    if (tras >= 0 && viable[tras][j + 1]) {
                        for (t in i until i + largo) puedePintada[t] = true
                        // La casilla separadora, si existe, queda vacía.
                        if (i + largo < n) puedeVacia[i + largo] = true
                        alcanzable[tras][j + 1] = true
                    }
                }
            }
        }

        val resultado = ArrayList<Cell>(n)
        for (i in 0 until n) {
            resultado += when {
                puedePintada[i] && !puedeVacia[i] -> Cell.FILLED
                !puedePintada[i] && puedeVacia[i] -> Cell.CROSSED
                puedePintada[i] && puedeVacia[i] -> Cell.UNKNOWN
                else -> return null
            }
        }
        return resultado
    }

    /**
     * Si un bloque de [largo] cabe empezando en [inicio], devuelve el índice desde el
     * que seguir colocando (saltando la casilla separadora). Devuelve -1 si no cabe.
     */
    private fun sitioTrasBloque(linea: List<Cell>, inicio: Int, largo: Int, n: Int): Int {
        if (inicio + largo > n) return -1
        for (i in inicio until inicio + largo) {
            if (linea[i] == Cell.CROSSED) return -1
        }
        val fin = inicio + largo
        if (fin == n) return n
        // Justo después de un bloque tiene que haber hueco.
        if (linea[fin] == Cell.FILLED) return -1
        return fin + 1
    }
}
