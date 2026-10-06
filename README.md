# Pixa

**Nonogramas** (picture logic / "pinta por números") para Android. Nativo en **Kotlin + Jetpack
Compose**, sin servidor, sin cuentas y sin conexión obligatoria: los puzzles se **generan en el
móvil** y se garantiza que tienen **solución única y deducible por lógica**, nunca a base de
adivinar.

<p align="center">
  <em>5 × 5 · 10 × 10 · 15 × 15 · un puzzle del día igual para todo el mundo</em>
</p>

---

## Por qué este juego

Un nonograma no necesita texto (así que vale igual en España que en Japón), no necesita
ilustraciones (el dibujo **es** la rejilla), se juega en sesiones largas de 10–20 minutos y se
genera infinito sin coste de servidor. Para un juego hecho por una persona, es de lo poco que
cumple las cuatro cosas a la vez.

---

## La parte interesante: el motor

El código que de verdad tiene chicha está en [`engine/`](app/src/main/java/com/riscart/pixa/engine).

### Resolver una línea sin probar combinaciones

`LineSolver` deduce lo que se puede saber **con certeza** de una fila o columna usando
programación dinámica, no fuerza bruta:

1. Tabla `viable[i][j]`: ¿es posible colocar las pistas a partir de la pista `j` en la casilla `i`?
2. Pasada hacia delante marcando, para cada casilla, si **puede** estar pintada y si **puede**
   estar vacía en alguna colocación válida.
3. Si solo puede estar pintada → pintada. Si solo puede estar vacía → tachada. Si puede las dos,
   se queda sin decidir.

Esto es lo que da el clásico solapamiento: una línea de 5 con la pista `[4]` tiene siempre
pintadas las tres del medio, sin saber todavía dónde empieza el bloque.

```
?????  +  [4]   →   ?###?
```

### Generar puzzles que no obliguen a adivinar

`Generator` crea una rejilla aleatoria, calcula sus pistas y **solo acepta el puzzle si el
solucionador lógico es capaz de terminarlo** (`Solver` alterna filas y columnas hasta que deja de
avanzar). Si se atasca, lo descarta y prueba otro. Por eso:

- la solución es **única** (si hubiera dos, la lógica se atascaría);
- **nunca** hace falta adivinar y volver atrás.

Es la diferencia entre un nonograma bien hecho y uno que da rabia.

Además filtra los puzzles "feos": ni demasiado vacíos ni demasiado llenos, y sin filas enteras en
blanco de más.

### Tests

23 tests en [`EngineTest.kt`](app/src/test/java/com/riscart/pixa/engine/EngineTest.kt) con una
notación compacta (`"#?."` = pintada / sin decidir / tachada): deducciones concretas, pistas
imposibles, la garantía de que el solucionador **nunca** se contradice, puzzles generados a 5, 10
y 15 que el solucionador lógico resuelve enteros, y determinismo por semilla.

```bash
./gradlew test
```

---

## Decisiones de diseño

- **Pintar está validado; tachar es libre.** Si pintas donde no toca, cuenta error y la casilla
  se tacha sola. Así el tablero nunca se queda en un estado sin solución y no hay que llevar la
  cuenta mentalmente de los fallos. Las cruces son apuntes tuyos y no se penalizan.
- **El tablero se dibuja en un `Canvas`**, no con 225 composables. A 15 × 15, arrastrar el dedo
  pintando tiene que ir fino.
- **El puzzle del día sale de la fecha** (`semillaDelDia()`), no de un servidor: todo el mundo
  juega el mismo y la app sigue funcionando sin conexión.
- **El reloj se para** cuando la app pasa a segundo plano: ver un anuncio o atender una llamada no
  te arruina el récord.
- **Al ganar, el cartel no tapa el dibujo.** Ocupa el sitio de los controles, porque el dibujo
  terminado es el premio.
- **Modo oscuro diseñado aparte**, no invertido.
- **SharedPreferences** para el progreso. Son cuatro enteros; Room o DataStore aquí sobran.

---

## Monetización

Dos vías, ambas con AdMob y pensadas para no espantar al jugador:

| Dónde | Tipo | Regla |
|---|---|---|
| Botón "Ver anuncio +2" | **Recompensado** | Solo aparece cuando te quedas sin pistas. Siempre voluntario. |
| Al terminar un puzzle | **Intersticial** | Una de cada **3** partidas, y **jamás** a mitad de una. |

El juego **nunca se bloquea por un anuncio**: si no carga, la recompensa se da igual
([`GestorAnuncios.kt`](app/src/main/java/com/riscart/pixa/anuncios/GestorAnuncios.kt)).

### Poner tus identificadores reales

Ahora mismo están los **de prueba de Google** (funcionan sin cuenta y no generan ingresos). Para
cobrar:

1. Crea una cuenta en [AdMob](https://admob.google.com) y da de alta la app.
2. Crea dos bloques de anuncios: uno **recompensado** y uno **intersticial**.
3. Sustituye:

   | Dónde | Qué |
   |---|---|
   | `AndroidManifest.xml` → `APPLICATION_ID` | `ca-app-pub-XXXX~YYYY` |
   | `GestorAnuncios.kt` → `ID_RECOMPENSADO` | `ca-app-pub-XXXX/ZZZZ` |
   | `GestorAnuncios.kt` → `ID_INTERSTICIAL` | `ca-app-pub-XXXX/WWWW` |

> ⚠️ Con los identificadores reales, **no pulses tus propios anuncios**: Google lo detecta como
> tráfico inválido y cierra cuentas por eso. Para probar, usa siempre los de prueba o registra tu
> móvil como dispositivo de test.

---

## Compilar

Requisitos: **JDK 21** y el SDK de Android (`compileSdk 36`).

```bash
./gradlew assembleDebug        # APK de depuración
./gradlew test                 # tests del motor
./gradlew bundleRelease        # AAB firmado para Play (necesita keystore.properties)
```

### Firma

El AAB de release se firma con una clave que **no está en el repositorio**:

```bash
keytool -genkeypair -v -keystore pixa-release.jks -alias pixa \
        -keyalg RSA -keysize 2048 -validity 10000
cp keystore.properties.example keystore.properties   # y rellénalo
```

`.gitignore` ya excluye `*.jks` y `keystore.properties`.

> **Guarda el `.jks` y sus contraseñas en un sitio seguro.** Si pierdes la clave de subida se
> puede pedir un reseteo a Google, pero es un trámite lento y evitable.

---

## Publicar en Google Play

1. **Cuenta de desarrollador** — 25 $ (pago único) en [Play Console](https://play.google.com/console).
2. **Testers**: una cuenta **personal** nueva necesita **12 testers que prueben la app 14 días
   seguidos** antes de poder pasar a producción. Es el requisito que más tiempo come: móntalo
   cuanto antes con un grupo de prueba cerrado. (Las cuentas de **organización** no lo tienen.)
3. **Sube el AAB** de `app/build/outputs/bundle/release/`.
4. **Política de privacidad**: obligatoria al llevar anuncios. Tienes una plantilla lista en
   [`PRIVACIDAD.md`](PRIVACIDAD.md); súbela a una URL pública (GitHub Pages vale) y pégala en la
   ficha.
5. **Seguridad de los datos**: declara que un SDK de terceros (AdMob) recoge **identificador de
   publicidad** y **datos de uso/diagnóstico**. La app en sí no recoge nada: el progreso se queda
   en el móvil.
6. **Anuncios**: marca "Sí, contiene anuncios".
7. **Clasificación por edades**, público objetivo y ficha (título, descripción, icono 512×512,
   gráfico de cabecera 1024×500 y capturas).

### Si el público va a incluir menores

Hay que activar el tratamiento para menores en AdMob y pasar `TagForChildDirectedTreatment`. Lo
más sencillo es declarar la app para **mayores de 13** y evitarse ese bloque.

---

## Expectativas realistas

Esto hay que decirlo claro: **la app no es la que da dinero, la retención y la visibilidad sí**.
Un juego de puzzles con anuncios ronda los **0,5–2 € por cada 1.000 partidas** en España, así que
sin instalaciones no hay negocio. Lo que de verdad mueve la aguja:

- **el puzzle del día** (da motivo para volver mañana — por eso existe la racha);
- **las primeras 20 valoraciones**, que son las que hacen que Play empiece a enseñarla;
- unas **capturas buenas** en la ficha, que es lo único que ve el 90 % de la gente;
- y publicar **actualizaciones pequeñas y seguidas** los primeros meses.

---

## Siguientes pasos naturales

- Compartir el dibujo terminado como imagen (es el mejor marketing gratis de este género).
- Puzzles con imagen real detrás (de un conjunto de dibujos a mano) además de los generados.
- Guardar la partida a medias al salir.
- Logros y estadísticas por tamaño.

---

## Licencia

MIT
