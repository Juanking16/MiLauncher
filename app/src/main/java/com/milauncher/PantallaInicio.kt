package com.milauncher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PantallaInicio(acciones: Acciones, resumeTick: Int) {
    val ctx = LocalContext.current
    val store = remember { SettingsStore(ctx.applicationContext) }
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
        val nuevas = withContext(Dispatchers.Default) { acciones.clavesApps() }
        val actuales = appsBase.map { it.packageName + "/" + it.activityName }.toSet()
        if (nuevas != actuales) appsBase = acciones.cargarApps()
    }

    // Aplica el paquete de iconos elegido
    LaunchedEffect(appsBase, ajustes.iconPack, cargado) {
        if (cargado) {
            apps = if (ajustes.iconPack.isBlank()) appsBase
            else acciones.aplicarPack(appsBase, ajustes.iconPack)
        }
    }

    // Detecta paquetes de iconos y avisa si hay uno nuevo
    LaunchedEffect(resumeTick, cargado) {
        if (cargado) {
            val detectados = withContext(Dispatchers.Default) { acciones.detectarPacks() }
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
        ctx.packageManager.getApplicationIcon(ctx.packageName).toBitmap(128, 128).asImageBitmap()
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
    val fondoVista = if (pantalla != 0 || ajustes.fondoModo == 1) Paleta.FondoAmoled
    else colorBase.copy(alpha = ajustes.fondoOscuro)

    fun aviso(texto: String) {
        Toast.makeText(ctx, texto, Toast.LENGTH_SHORT).show()
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

    TemaLauncher(ajustes.acento) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fondoVista)
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
                    onBuscarPacks = { acciones.buscarPacks() }
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
                                            acciones.openApp(app, ajustes.animacion, r)
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
                                    onAbrir = { app, r ->
                                        acciones.openApp(app, ajustes.animacion, r)
                                    },
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
                                        acciones.openApp(app, ajustes.animacion, r)
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
                            acciones.infoApp(sel)
                            menuApp = null
                        })
                        if (!sel.sistema) {
                            add(Opcion("Desinstalar", true) {
                                acciones.desinstalar(sel)
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