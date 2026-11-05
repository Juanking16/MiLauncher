package com.milauncher

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap,
    val sistema: Boolean = false
)

data class Atajo(val etiqueta: String, val icono: ImageBitmap, val accion: (Rect) -> Unit)

data class Elemento(val atajo: Atajo, val app: AppItem?)

data class Opcion(val texto: String, val peligro: Boolean = false, val accion: () -> Unit)

// Guarda la posición de un icono en pantalla sin provocar recomposiciones
class Caja { var r: Rect = Rect.Zero }

val coloresFondo = listOf(
    Color(0xFF000000),
    Color(0xFF0B1B3A),
    Color(0xFF2A0B3A),
    Color(0xFF0B2A1B),
    Color(0xFF2B2B2B),
    Color(0xFF3A0B0B),
    Color(0xFF3A200B),
    Color(0xFF3A0B2A),
    Color(0xFF0B3A3A),
    Color(0xFF2E1F14)
)
val nombresColor = listOf(
    "Negro", "Azul", "Violeta", "Verde", "Gris",
    "Rojo", "Naranja", "Rosa", "Turquesa", "Marrón"
)

val paletaReloj = listOf(
    Color(0xFFFFFFFF), Color(0xFFFFE082), Color(0xFFFBBF24), Color(0xFFFF8A65),
    Color(0xFFFF5252), Color(0xFFF48FB1), Color(0xFFCE93D8), Color(0xFF9FA8DA),
    Color(0xFF64B5F6), Color(0xFF4DD0E1), Color(0xFF80CBC4), Color(0xFFA5D6A7),
    Color(0xFFC5E1A5), Color(0xFFBDBDBD), Color(0xFF000000)
)

val sombraTexto = Shadow(color = Color(0xAA000000), offset = Offset(0f, 2f), blurRadius = 6f)

val dockClaves = listOf(
    listOf("Teléfono", "Telefono", "Phone", "Llamadas"),
    listOf("Mensajes", "Messages"),
    listOf("Cámara", "Camara", "Camera"),
    listOf("Brave", "Chrome", "Internet", "Samsung Internet", "Firefox")
)
val inicioClaves = listOf(
    listOf("Galería", "Galeria", "Gallery"),
    listOf("Reloj", "Clock"),
    listOf("Calendario", "Calendar"),
    listOf("Mis archivos", "Files", "Archivos"),
    listOf("WhatsApp"),
    listOf("Instagram"),
    listOf("Ajustes", "Settings"),
    listOf("Play Store", "Google Play Store")
)

// Primero busca el nombre exacto y, si no lo encuentra, uno que empiece igual
fun buscarApp(apps: List<AppItem>, claves: List<String>): AppItem? =
    claves.firstNotNullOfOrNull { c -> apps.firstOrNull { it.label.equals(c, ignoreCase = true) } }
        ?: claves.firstNotNullOfOrNull { c -> apps.firstOrNull { it.label.startsWith(c, ignoreCase = true) } }

fun formaIcono(i: Int): Shape = when (i) {
    1 -> CircleShape
    2 -> RoundedCornerShape(14.dp)
    3 -> RoundedCornerShape(4.dp)
    else -> RectangleShape
}

// Efecto al pasar de página: 0 Deslizar, 1 Libro, 2 Cubo, 3 Zoom
fun Modifier.efectoPagina(estado: PagerState, pagina: Int, estilo: Int): Modifier =
    graphicsLayer {
        val p = ((pagina - estado.currentPage) - estado.currentPageOffsetFraction)
            .coerceIn(-1f, 1f)
        val a = abs(p)
        when (estilo) {
            1 -> {
                cameraDistance = 12f * density
                transformOrigin = TransformOrigin(if (p < 0f) 1f else 0f, 0.5f)
                rotationY = -p * 60f
                alpha = 1f - a * 0.35f
            }
            2 -> {
                cameraDistance = 12f * density
                transformOrigin = TransformOrigin(if (p < 0f) 1f else 0f, 0.5f)
                rotationY = p * 90f
                alpha = 1f - a * 0.2f
            }
            3 -> {
                scaleX = 1f - 0.25f * a
                scaleY = 1f - 0.25f * a
                alpha = 1f - a * 0.6f
            }
            else -> {}
        }
    }

// Pantallas de ajustes: 0 cerrado, 1 menú, 2 Apariencia, 4 Animaciones, 5 Reloj
fun atrasDe(p: Int): Int = when (p) {
    5 -> 2
    2 -> 1
    4 -> 1
    else -> 0
}

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HomeScreen() }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    private fun clavesApps(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map { it.activityInfo.packageName + "/" + it.activityInfo.name }
            .toSet()
    }

    private suspend fun loadApps(): List<AppItem> = withContext(Dispatchers.Default) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map {
                val flags = it.activityInfo.applicationInfo.flags
                val esSistema = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                AppItem(
                    label = it.loadLabel(packageManager).toString(),
                    packageName = it.activityInfo.packageName,
                    activityName = it.activityInfo.name,
                    icon = it.loadIcon(packageManager).toBitmap(128, 128).asImageBitmap(),
                    sistema = esSistema
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private suspend fun aplicarPack(base: List<AppItem>, pack: String): List<AppItem> =
        withContext(Dispatchers.Default) {
            IconPacks(applicationContext).aplicar(base, pack)
        }

    private fun openApp(app: AppItem, estilo: Int, origen: Rect? = null) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val vista = window.decorView
        val x = origen?.left?.toInt() ?: (vista.width / 2)
        val y = origen?.top?.toInt() ?: (vista.height / 2)
        val ancho = origen?.width?.toInt() ?: 1
        val alto = origen?.height?.toInt() ?: 1

        val opciones = when (estilo) {
            1 -> ActivityOptions.makeCustomAnimation(this, R.anim.zoom_in, R.anim.zoom_out)
            2 -> ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
            3 -> ActivityOptions.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
            4 -> ActivityOptions.makeScaleUpAnimation(vista, x, y, ancho, alto)
            5 -> ActivityOptions.makeClipRevealAnimation(vista, x, y, ancho, alto)
            6 -> ActivityOptions.makeCustomAnimation(this, R.anim.slide_up_in, R.anim.slide_up_out)
            7 -> ActivityOptions.makeCustomAnimation(this, R.anim.bounce_in, R.anim.bounce_out)
            8 -> ActivityOptions.makeCustomAnimation(this, R.anim.spin_in, R.anim.spin_out)
            else -> null
        }
        startActivity(intent, opciones?.toBundle())
    }

    private fun desinstalar(app: AppItem) {
        try {
            val i = Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.packageName))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el desinstalador", Toast.LENGTH_SHORT).show()
        }
    }

    private fun infoApp(app: AppItem) {
        try {
            val i = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + app.packageName)
            )
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir la información", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buscarPacks() {
        try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=icon%20pack&c=apps"))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (e: Exception) {
            try {
                val i = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/search?q=icon%20pack&c=apps")
                )
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            } catch (e2: Exception) {
                Toast.makeText(this, "No se pudo abrir la tienda", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Composable
    fun HomeScreen() {
        val store = remember { SettingsStore(applicationContext) }
        val ajustesCarga by store.flow.collectAsState(initial = null)
        val cargado = ajustesCarga != null
        val ajustes = ajustesCarga ?: Ajustes()
        val scope = rememberCoroutineScope()

        var appsBase by remember { mutableStateOf<List<AppItem>>(emptyList()) }
        var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
        var packs by remember { mutableStateOf<List<PaqueteIconos>>(emptyList()) }
        var pantalla by remember { mutableStateOf(0) }
        var menuApp by remember { mutableStateOf<AppItem?>(null) }
        var packNuevo by remember { mutableStateOf<PaqueteIconos?>(null) }

        // Lista de apps: se recarga solo si se instaló o desinstaló algo
        LaunchedEffect(resumeTick) {
            val nuevas = withContext(Dispatchers.Default) { clavesApps() }
            val actuales = appsBase.map { it.packageName + "/" + it.activityName }.toSet()
            if (nuevas != actuales) appsBase = loadApps()
        }

        // Aplica el paquete de iconos elegido
        LaunchedEffect(appsBase, ajustes.iconPack, cargado) {
            if (cargado) {
                apps = if (ajustes.iconPack.isBlank()) appsBase
                else aplicarPack(appsBase, ajustes.iconPack)
            }
        }

        // Detecta paquetes de iconos y avisa si hay uno nuevo
        LaunchedEffect(resumeTick, cargado) {
            if (cargado) {
                val detectados = withContext(Dispatchers.Default) {
                    IconPacks(applicationContext).detectar()
                }
                packs = detectados
                val vistos = ajustes.packsVistos
                if (vistos == null) {
                    store.guardar(
                        ajustes.copy(packsVistos = detectados.joinToString(",") { it.paquete })
                    )
                } else {
                    val conocidos = vistos.split(",").filter { it.isNotBlank() }
                    val nuevo = detectados.firstOrNull { it.paquete !in conocidos }
                    if (nuevo != null && packNuevo == null) packNuevo = nuevo
                }
            }
        }

        // Atajos del inicio y del dock (automáticos hasta que los cambies)
        val dockAuto = remember(apps) {
            dockClaves.mapNotNull { buscarApp(apps, it) }.distinctBy { it.packageName }
        }
        val dockCfg = ajustes.dockPkgs
        val inicioCfg = ajustes.inicioPkgs
        val dockApps = remember(apps, dockCfg) {
            if (dockCfg == null) dockAuto
            else dockCfg.split(",").filter { it.isNotBlank() }
                .mapNotNull { p -> apps.firstOrNull { it.packageName == p } }
        }
        val inicioApps = remember(apps, inicioCfg, dockApps) {
            if (inicioCfg == null) {
                inicioClaves.mapNotNull { buscarApp(apps, it) }
                    .distinctBy { it.packageName }
                    .filter { a -> dockApps.none { d -> d.packageName == a.packageName } }
                    .take(7)
            } else {
                inicioCfg.split(",").filter { it.isNotBlank() }
                    .mapNotNull { p -> apps.firstOrNull { it.packageName == p } }
            }
        }
        val iconoPropio = remember {
            packageManager.getApplicationIcon(packageName).toBitmap(128, 128).asImageBitmap()
        }

        // Páginas: la 0 es el inicio y las demás tienen todas las apps
        val columnasPagina = ajustes.columnas.coerceAtLeast(3)
        val filasPagina = 5
        val porPagina = columnasPagina * filasPagina
        val paginasApps = if (apps.isEmpty()) 0 else (apps.size + porPagina - 1) / porPagina
        val totalPaginas = 1 + paginasApps
        val pagerState = rememberPagerState(pageCount = { totalPaginas })

        val colorBase = coloresFondo[ajustes.colorFondo.coerceIn(0, coloresFondo.size - 1)]
        val forma = formaIcono(ajustes.forma)
        val alineacionReloj = when (ajustes.relojPosicion) {
            1 -> Alignment.TopCenter
            2 -> Alignment.TopEnd
            3 -> Alignment.Center
            else -> Alignment.TopStart
        }

        // Acciones del menú de mantener pulsado
        fun aviso(texto: String) {
            Toast.makeText(applicationContext, texto, Toast.LENGTH_SHORT).show()
        }

        fun guardarListas(inicio: List<AppItem>, dock: List<AppItem>) {
            scope.launch {
                store.guardar(
                    ajustes.copy(
                        inicioPkgs = inicio.joinToString(",") { it.packageName },
                        dockPkgs = dock.joinToString(",") { it.packageName }
                    )
                )
            }
        }

        fun aInicio(app: AppItem) {
            val yaEsta = inicioApps.any { it.packageName == app.packageName }
            if (!yaEsta && inicioApps.size >= 11) {
                aviso("El inicio está lleno")
                return
            }
            guardarListas(
                inicioApps.filter { it.packageName != app.packageName } + app,
                dockApps.filter { it.packageName != app.packageName }
            )
        }

        fun aDock(app: AppItem) {
            val yaEsta = dockApps.any { it.packageName == app.packageName }
            if (!yaEsta && dockApps.size >= 5) {
                aviso("El dock está lleno (máximo 5)")
                return
            }
            guardarListas(
                inicioApps.filter { it.packageName != app.packageName },
                dockApps.filter { it.packageName != app.packageName } + app
            )
        }

        fun quitar(app: AppItem) {
            guardarListas(
                inicioApps.filter { it.packageName != app.packageName },
                dockApps.filter { it.packageName != app.packageName }
            )
        }

        fun mantener(app: AppItem?): (() -> Unit)? {
            if (app == null) return null
            return { menuApp = app }
        }

        fun resolverPack(p: PaqueteIconos, aplicar: Boolean) {
            val vistos = (ajustes.packsVistos ?: "").split(",").filter { it.isNotBlank() }
            val todos = (vistos + p.paquete).distinct().joinToString(",")
            scope.launch {
                if (aplicar) store.guardar(ajustes.copy(iconPack = p.paquete, packsVistos = todos))
                else store.guardar(ajustes.copy(packsVistos = todos))
            }
            packNuevo = null
        }

        BackHandler(enabled = pantalla != 0) { pantalla = atrasDe(pantalla) }
        BackHandler(enabled = pantalla == 0 && pagerState.currentPage != 0) {
            scope.launch { pagerState.animateScrollToPage(0) }
        }
        BackHandler(enabled = menuApp != null) { menuApp = null }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorBase.copy(alpha = ajustes.fondoOscuro))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (pantalla != 0) {
                PantallaAjustes(
                    pantalla = pantalla,
                    ajustes = ajustes,
                    packs = packs,
                    onCambio = { nuevo -> scope.launch { store.guardar(nuevo) } },
                    onIr = { pantalla = it },
                    onBuscarPacks = { buscarPacks() }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { pagina ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .efectoPagina(pagerState, pagina, ajustes.transPagina)
                        ) {
                            if (pagina == 0) {
                                if (ajustes.mostrarReloj) {
                                    Reloj(
                                        ajustes = ajustes,
                                        modifier = Modifier
                                            .align(alineacionReloj)
                                            .padding(horizontal = 28.dp, vertical = 36.dp)
                                    )
                                }
                                val elementos = listOf(
                                    Elemento(
                                        Atajo("Juan Launcher", iconoPropio) { pantalla = 1 },
                                        null
                                    )
                                ) + inicioApps.map { app ->
                                    Elemento(
                                        Atajo(app.label, app.icon) { r ->
                                            openApp(app, ajustes.animacion, r)
                                        },
                                        app
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                ) {
                                    elementos.chunked(4).forEach { fila ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                        ) {
                                            fila.forEach { el ->
                                                IconoAtajo(
                                                    atajo = el.atajo,
                                                    tamano = 58,
                                                    forma = forma,
                                                    mostrarNombre = true,
                                                    modifier = Modifier.weight(1f),
                                                    alMantener = mantener(el.app)
                                                )
                                            }
                                            repeat(4 - fila.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            } else {
                                val desde = (pagina - 1) * porPagina
                                PaginaApps(
                                    lista = apps.drop(desde).take(porPagina),
                                    columnas = columnasPagina,
                                    filas = filasPagina,
                                    tamano = ajustes.tamanoIcono,
                                    forma = forma,
                                    mostrarNombre = ajustes.mostrarNombres,
                                    onAbrir = { app, r -> openApp(app, ajustes.animacion, r) },
                                    onMantener = { app -> menuApp = app }
                                )
                            }
                        }
                    }

                    Puntos(total = totalPaginas, actual = pagerState.currentPage)

                    if (dockApps.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .clip(RoundedCornerShape(30.dp))
                                .background(Color(0x33FFFFFF))
                                .padding(vertical = 10.dp)
                        ) {
                            dockApps.forEach { app ->
                                IconoAtajo(
                                    atajo = Atajo(app.label, app.icon) { r ->
                                        openApp(app, ajustes.animacion, r)
                                    },
                                    tamano = 56,
                                    forma = forma,
                                    mostrarNombre = false,
                                    modifier = Modifier.weight(1f),
                                    alMantener = mantener(app)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Menú al mantener pulsada una app
                val sel = menuApp
                if (sel != null) {
                    val enInicio = inicioApps.any { it.packageName == sel.packageName }
                    val enDock = dockApps.any { it.packageName == sel.packageName }
                    val opciones = buildList<Opcion> {
                        if (!enInicio) {
                            add(Opcion(if (enDock) "Mover al inicio" else "Agregar al inicio") {
                                aInicio(sel)
                                menuApp = null
                            })
                        }
                        if (!enDock) {
                            add(Opcion(if (enInicio) "Mover al dock" else "Agregar al dock") {
                                aDock(sel)
                                menuApp = null
                            })
                        }
                        if (enInicio || enDock) {
                            add(Opcion(if (enDock) "Quitar del dock" else "Quitar del inicio") {
                                quitar(sel)
                                menuApp = null
                            })
                        }
                        add(Opcion("Información de la app") {
                            infoApp(sel)
                            menuApp = null
                        })
                        if (!sel.sistema) {
                            add(Opcion("Desinstalar", true) {
                                desinstalar(sel)
                                menuApp = null
                            })
                        }
                    }
                    MenuApp(app = sel, opciones = opciones, onCerrar = { menuApp = null })
                }

                // Aviso cuando se instala un paquete de iconos
                val nuevoPack = packNuevo
                if (nuevoPack != null && menuApp == null) {
                    AvisoPack(
                        nombre = nuevoPack.nombre,
                        onAplicar = { resolverPack(nuevoPack, true) },
                        onCerrar = { resolverPack(nuevoPack, false) }
                    )
                }
            }
        }
    }
}

@Composable
fun MenuApp(app: AppItem, opciones: List<Opcion>, onCerrar: () -> Unit) {
    val fuenteFondo = remember { MutableInteractionSource() }
    val fuenteTarjeta = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable(interactionSource = fuenteFondo, indication = null) { onCerrar() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .widthIn(min = 260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1F1F24))
                .clickable(interactionSource = fuenteTarjeta, indication = null) { }
                .padding(vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            opciones.forEach { op ->
                Text(
                    text = op.texto,
                    color = if (op.peligro) Color(0xFFFF6B6B) else Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { op.accion() }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
fun AvisoPack(nombre: String, onAplicar: () -> Unit, onCerrar: () -> Unit) {
    val fuenteFondo = remember { MutableInteractionSource() }
    val fuenteTarjeta = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable(interactionSource = fuenteFondo, indication = null) { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1F1F24))
                .clickable(interactionSource = fuenteTarjeta, indication = null) { }
                .padding(24.dp)
        ) {
            Text("Nuevo paquete de iconos", color = Color.White, fontSize = 19.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Instalaste «" + nombre + "». ¿Quieres usarlo en Juan Launcher?",
                color = Color(0xCCFFFFFF),
                fontSize = 15.sp
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Ahora no",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { onCerrar() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
                Text(
                    text = "Aplicar",
                    color = Color(0xFFFBBF24),
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { onAplicar() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
fun PaginaApps(
    lista: List<AppItem>,
    columnas: Int,
    filas: Int,
    tamano: Int,
    forma: Shape,
    mostrarNombre: Boolean,
    onAbrir: (AppItem, Rect) -> Unit,
    onMantener: (AppItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        repeat(filas) { fila ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                repeat(columnas) { col ->
                    val app = lista.getOrNull(fila * columnas + col)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app != null) {
                            IconoAtajo(
                                atajo = Atajo(app.label, app.icon) { r -> onAbrir(app, r) },
                                tamano = tamano,
                                forma = forma,
                                mostrarNombre = mostrarNombre,
                                modifier = Modifier.fillMaxWidth(),
                                alMantener = { onMantener(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Puntos(total: Int, actual: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(total.coerceAtMost(12)) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (i == actual) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (i == actual) Color.White else Color(0x66FFFFFF))
            )
        }
    }
}

@Composable
fun Reloj(ajustes: Ajustes, modifier: Modifier = Modifier) {
    var ahora by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            ahora = System.currentTimeMillis()
            delay(10_000)
        }
    }
    val patron = if (ajustes.relojFormato24) "HH:mm" else "h:mm a"
    val hora = remember(ahora, patron) {
        SimpleDateFormat(patron, Locale.getDefault()).format(Date(ahora))
    }
    val fecha = remember(ahora) {
        SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es"))
            .format(Date(ahora))
            .replaceFirstChar { it.uppercase() }
    }
    val color = Color(ajustes.relojColor)
    val peso = when (ajustes.relojGrosor) {
        1 -> FontWeight.Normal
        2 -> FontWeight.Bold
        else -> FontWeight.Light
    }
    val fuente = when (ajustes.relojFuente) {
        1 -> FontFamily.Serif
        2 -> FontFamily.Monospace
        3 -> FontFamily.Cursive
        else -> FontFamily.Default
    }
    val sombra = if (ajustes.relojSombra) sombraTexto else null
    val alineacion = when (ajustes.relojPosicion) {
        1, 3 -> Alignment.CenterHorizontally
        2 -> Alignment.End
        else -> Alignment.Start
    }

    Column(modifier = modifier, horizontalAlignment = alineacion) {
        Text(
            text = hora,
            color = color,
            fontSize = ajustes.relojTamano.sp,
            fontWeight = peso,
            fontFamily = fuente,
            style = TextStyle(shadow = sombra)
        )
        if (ajustes.relojFecha) {
            Text(
                text = fecha,
                color = color,
                fontSize = (ajustes.relojTamano / 3.5f).coerceAtLeast(13f).sp,
                fontFamily = fuente,
                style = TextStyle(shadow = sombra)
            )
        }
    }
}

@Composable
fun IconoAtajo(
    atajo: Atajo,
    tamano: Int,
    forma: Shape,
    mostrarNombre: Boolean,
    modifier: Modifier = Modifier,
    alMantener: (() -> Unit)? = null
) {
    val caja = remember { Caja() }
    val haptic = LocalHapticFeedback.current
    val accion by rememberUpdatedState(atajo.accion)
    val mantener by rememberUpdatedState(alMantener)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { accion(caja.r) },
                    onLongPress = {
                        val m = mantener
                        if (m != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            m()
                        }
                    }
                )
            }
            .padding(vertical = 4.dp)
    ) {
        Image(
            bitmap = atajo.icono,
            contentDescription = atajo.etiqueta,
            modifier = Modifier
                .size(tamano.dp)
                .clip(forma)
                .onGloballyPositioned { caja.r = it.boundsInWindow() }
        )
        if (mostrarNombre) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = atajo.etiqueta,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = sombraTexto)
            )
        }
    }
}

@Composable
fun Selector(
    titulo: String,
    opciones: List<String>,
    actual: Int,
    onElegir: (Int) -> Unit
) {
    Spacer(Modifier.height(14.dp))
    Text(titulo, color = Color.White)
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        opciones.forEachIndexed { i, nombre ->
            Text(
                text = nombre,
                color = if (actual == i) Color(0xFFFBBF24) else Color.White,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable { onElegir(i) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun Interruptor(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = valor, onCheckedChange = onCambio)
    }
}

@Composable
fun Muestra(color: Color, elegido: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (elegido) 3.dp else 1.dp,
                color = if (elegido) Color(0xFFFBBF24) else Color(0x55FFFFFF),
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
fun ItemMenu(texto: String, onClick: () -> Unit) {
    Text(
        text = "$texto  ›",
        color = Color.White,
        fontSize = 18.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp)
    )
}

@Composable
fun PantallaAjustes(
    pantalla: Int,
    ajustes: Ajustes,
    packs: List<PaqueteIconos>,
    onCambio: (Ajustes) -> Unit,
    onIr: (Int) -> Unit,
    onBuscarPacks: () -> Unit
) {
    var columnas by remember { mutableStateOf(ajustes.columnas.toFloat()) }
    var tamano by remember { mutableStateOf(ajustes.tamanoIcono.toFloat()) }
    var fondo by remember { mutableStateOf(ajustes.fondoOscuro) }
    var relojTam by remember { mutableStateOf(ajustes.relojTamano.toFloat()) }

    val titulo = when (pantalla) {
        1 -> "Ajustes"
        2 -> "Apariencia"
        5 -> "Reloj"
        else -> "Animaciones"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "← $titulo",
            color = Color.White,
            fontSize = 22.sp,
            modifier = Modifier
                .clickable { onIr(atrasDe(pantalla)) }
                .padding(vertical = 8.dp)
        )
        Spacer(Modifier.height(8.dp))

        when (pantalla) {
            1 -> {
                ItemMenu("Apariencia") { onIr(2) }
                ItemMenu("Animaciones") { onIr(4) }
            }
            2 -> {
                ItemMenu("Reloj") { onIr(5) }

                val indicePack = packs.indexOfFirst { it.paquete == ajustes.iconPack }
                    .let { if (it < 0) 0 else it + 1 }
                Selector(
                    "Paquete de iconos",
                    listOf("Predeterminado") + packs.map { it.nombre },
                    indicePack
                ) { i ->
                    onCambio(ajustes.copy(iconPack = if (i == 0) "" else packs[i - 1].paquete))
                }
                if (packs.isEmpty()) {
                    Text(
                        "No hay paquetes de iconos instalados.",
                        color = Color(0x99FFFFFF),
                        fontSize = 12.sp
                    )
                }
                ItemMenu("Buscar paquetes en Play Store") { onBuscarPacks() }

                Selector("Forma de iconos", listOf("Original", "Círculo", "Redondeado", "Cuadrado"), ajustes.forma) {
                    onCambio(ajustes.copy(forma = it))
                }
                Selector("Color de fondo", nombresColor, ajustes.colorFondo) {
                    onCambio(ajustes.copy(colorFondo = it))
                }
                Spacer(Modifier.height(8.dp))
                Text("Oscuridad del fondo: ${(fondo * 100).toInt()}%", color = Color.White)
                Slider(
                    value = fondo,
                    onValueChange = { fondo = it },
                    onValueChangeFinished = { onCambio(ajustes.copy(fondoOscuro = fondo)) },
                    valueRange = 0f..0.9f
                )
                Text("Columnas: ${columnas.toInt()}", color = Color.White)
                Slider(
                    value = columnas,
                    onValueChange = { columnas = it },
                    onValueChangeFinished = { onCambio(ajustes.copy(columnas = columnas.toInt())) },
                    valueRange = 3f..6f,
                    steps = 2
                )
                Text("Tamaño de iconos: ${tamano.toInt()}", color = Color.White)
                Slider(
                    value = tamano,
                    onValueChange = { tamano = it },
                    onValueChangeFinished = { onCambio(ajustes.copy(tamanoIcono = tamano.toInt())) },
                    valueRange = 36f..72f
                )
                Interruptor("Mostrar nombres de las apps", ajustes.mostrarNombres) {
                    onCambio(ajustes.copy(mostrarNombres = it))
                }
            }
            5 -> {
                Reloj(
                    ajustes = ajustes.copy(mostrarReloj = true),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
                Interruptor("Mostrar reloj en el inicio", ajustes.mostrarReloj) {
                    onCambio(ajustes.copy(mostrarReloj = it))
                }
                Interruptor("Mostrar fecha", ajustes.relojFecha) {
                    onCambio(ajustes.copy(relojFecha = it))
                }
                Interruptor("Sombra del texto", ajustes.relojSombra) {
                    onCambio(ajustes.copy(relojSombra = it))
                }
                Selector("Formato", listOf("24 horas", "12 horas"), if (ajustes.relojFormato24) 0 else 1) {
                    onCambio(ajustes.copy(relojFormato24 = it == 0))
                }
                Selector("Fuente", listOf("Normal", "Serif", "Monospace", "Cursiva"), ajustes.relojFuente) {
                    onCambio(ajustes.copy(relojFuente = it))
                }
                Selector("Grosor", listOf("Fino", "Normal", "Negrita"), ajustes.relojGrosor) {
                    onCambio(ajustes.copy(relojGrosor = it))
                }
                Selector(
                    "Posición",
                    listOf("Arriba izq.", "Arriba centro", "Arriba der.", "Centro"),
                    ajustes.relojPosicion
                ) { onCambio(ajustes.copy(relojPosicion = it)) }

                Spacer(Modifier.height(14.dp))
                Text("Tamaño: ${relojTam.toInt()}", color = Color.White)
                Slider(
                    value = relojTam,
                    onValueChange = { relojTam = it },
                    onValueChangeFinished = { onCambio(ajustes.copy(relojTamano = relojTam.toInt())) },
                    valueRange = 32f..96f
                )

                Spacer(Modifier.height(8.dp))
                Text("Color", color = Color.White)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    paletaReloj.forEach { c ->
                        Muestra(c, ajustes.relojColor == c.toArgb()) {
                            onCambio(ajustes.copy(relojColor = c.toArgb()))
                        }
                    }
                }

                val actual = Color(ajustes.relojColor)
                var rojo by remember(ajustes.relojColor) { mutableStateOf(actual.red) }
                var verde by remember(ajustes.relojColor) { mutableStateOf(actual.green) }
                var azul by remember(ajustes.relojColor) { mutableStateOf(actual.blue) }

                Spacer(Modifier.height(12.dp))
                Text("Color personalizado", color = Color(0x99FFFFFF), fontSize = 13.sp)
                Text("Rojo: ${(rojo * 255).toInt()}", color = Color.White)
                Slider(
                    value = rojo,
                    onValueChange = { rojo = it },
                    onValueChangeFinished = {
                        onCambio(ajustes.copy(relojColor = Color(rojo, verde, azul).toArgb()))
                    },
                    valueRange = 0f..1f
                )
                Text("Verde: ${(verde * 255).toInt()}", color = Color.White)
                Slider(
                    value = verde,
                    onValueChange = { verde = it },
                    onValueChangeFinished = {
                        onCambio(ajustes.copy(relojColor = Color(rojo, verde, azul).toArgb()))
                    },
                    valueRange = 0f..1f
                )
                Text("Azul: ${(azul * 255).toInt()}", color = Color.White)
                Slider(
                    value = azul,
                    onValueChange = { azul = it },
                    onValueChangeFinished = {
                        onCambio(ajustes.copy(relojColor = Color(rojo, verde, azul).toArgb()))
                    },
                    valueRange = 0f..1f
                )
            }
            else -> {
                Selector(
                    "Transición entre páginas",
                    listOf("Deslizar", "Libro", "Cubo", "Zoom"),
                    ajustes.transPagina
                ) { onCambio(ajustes.copy(transPagina = it)) }
                Text(
                    when (ajustes.transPagina) {
                        1 -> "Las páginas giran como las hojas de un libro."
                        2 -> "Las páginas giran como las caras de un cubo."
                        3 -> "La página se encoge y se desvanece al pasar."
                        else -> "Deslizamiento simple."
                    },
                    color = Color(0x99FFFFFF),
                    fontSize = 12.sp
                )
                Selector(
                    "Animación al abrir apps",
                    listOf(
                        "Ninguna", "Zoom", "Fade", "Deslizar", "Desde el icono",
                        "Revelar", "Subir", "Rebote", "Giro"
                    ),
                    ajustes.animacion
                ) { onCambio(ajustes.copy(animacion = it)) }
                Text(
                    when (ajustes.animacion) {
                        4 -> "La app crece desde el icono que tocas."
                        5 -> "La app se destapa desde el icono."
                        6 -> "La app sube desde abajo."
                        7 -> "La app aparece con un pequeño rebote."
                        8 -> "La app entra girando suavemente."
                        else -> "Animación simple de ventana."
                    },
                    color = Color(0x99FFFFFF),
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}