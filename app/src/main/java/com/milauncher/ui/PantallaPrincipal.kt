package com.milauncher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.appwidget.AppWidgetManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.milauncher.data.*
import com.milauncher.ui.MiLauncherTheme
import com.milauncher.ui.Tema
import com.milauncher.widgets.WidgetHostManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MiLauncherTheme { PantallaPrincipal(this@MainActivity) } }
    }
}

@Composable
fun PantallaPrincipal(activity: Activity) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    val repositorio = remember { AppRepository(context) }
    val iconLoader = remember { IconPackLoader(context) }
    val widgetHost = remember { WidgetHostManager(context) }

    val ajustes by store.flow.collectAsState(initial = Ajustes())
    val ocultas by store.ocultasFlow.collectAsState(initial = emptySet())
    val carpetas by store.carpetasFlow.collectAsState(initial = emptyList())
    val iconPack by store.iconPackFlow.collectAsState(initial = "")
    val widgetsGuardados by store.widgetsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var pantalla by remember { mutableStateOf("home") } // home | cajon | ajustes | ocultas | carpetas | widgets
    var busqueda by remember { mutableStateOf("") }
    var carpetaAbierta by remember { mutableStateOf<Carpeta?>(null) }
    var widgetPendiente by remember { mutableIntStateOf(-1) }

    val cargando = apps.isEmpty()

    LaunchedEffect(Unit) {
        widgetHost.empezarEscuchar()
        apps = repositorio.cargarApps()
    }
    DisposableEffect(Unit) {
        onDispose { widgetHost.pararEscuchar() }
    }

    // Rellenar widget recién enlazado
    LaunchedEffect(widgetPendiente) {
        if (widgetPendiente > 0) {
            val actuales = widgetsGuardados
            if (actuales.none { it.appWidgetId == widgetPendiente }) {
                store.guardarWidgets(actuales + WidgetPosicionado(widgetPendiente))
            }
            widgetPendiente = -1
        }
    }

    fun accionDe(g: AccionGesto) {
        when (g) {
            AccionGesto.ABRIR_CAJON -> { pantalla = "cajon"; busqueda = "" }
            AccionGesto.ABRIR_BUSCADOR -> { pantalla = "cajon"; busqueda = " " }
            AccionGesto.ABRIR_AJUSTES -> pantalla = "ajustes"
            AccionGesto.EXPANDIR_NOTIFICACIONES -> expandirNotificaciones()
            AccionGesto.ABRIR_ULTIMA_APP -> {}
            AccionGesto.NINGUNO -> {}
        }
    }

    fun aplicarIconos(lista: List<AppItem>): List<AppItem> {
        if (iconPack.isBlank()) return lista
        return lista.map { app ->
            val bitmap = iconLoader.cargarIcono(iconPack, app.packageName, ajustes.tamanoIcono * 2)
            if (bitmap != null) app.copy(icon = bitmap) else app
        }
    }

    val appsVisibles = remember(apps, ocultas, iconPack) { aplicarIconos(apps.filter { it.key !in ocultas }) }
    val appsFiltradas = remember(appsVisibles, busqueda) {
        val q = busqueda.trim()
        if (q.isEmpty()) appsVisibles else appsVisibles.filter { it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true) }
    }

    BackHandler(enabled = pantalla != "home" || carpetaAbierta != null) {
        when {
            carpetaAbierta != null -> carpetaAbierta = null
            pantalla != "home" -> pantalla = "home"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Tema.tema(ajustes.tema)["fondo"] ?: Tema.negro)
            .pointerInput(ajustes) {
                detectTapGestures(
                    onDoubleTap = { accionDe(ajustes.gestoDobleToque) },
                    onLongPress = { pantalla = "ajustes" }
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when (pantalla) {
            "home" -> HomeView(
                activity, ajustes, appsVisibles, carpetas,
                abrirCarpeta = { carpetaAbierta = it },
                abrirApp = { app -> scope.launch { store.setUltimaApp(app.key) }; repositorio.abrir(app) },
                onGestoArriba = { accionDe(ajustes.gestoArriba) },
                onGestoAbajo = { accionDe(ajustes.gestoAbajo) },
                onAjustes = { pantalla = "ajustes" },
                widgetHost = widgetHost,
                widgetsGuardados = widgetsGuardados,
                store = store,
                onAnadirWidget = { id -> widgetPendiente = id },
                onQuitarWidget = { id -> scope.launch { widgetHost.eliminar(id); store.guardarWidgets(widgetsGuardados.filter { it.appWidgetId != id }) } }
            )
            "cajon" -> CajonView(
                activity, ajustes, appsFiltradas, busqueda,
                onBusqueda = { busqueda = it },
                abrirApp = { app -> scope.launch { store.setUltimaApp(app.key) }; repositorio.abrir(app) },
                onCerrar = { pantalla = "home" },
                onOcultar = { key -> scope.launch { store.toggleOculta(key) } }
            )
            "ajustes" -> AjustesView(
                activity, store, ajustes, apps, ocultas, iconPack, iconLoader,
                onCerrar = { pantalla = "home" },
                irOcultas = { pantalla = "ocultas" },
                irWidgets = { pantalla = "widgets" }
            )
            "ocultas" -> OcultasView(store, apps, ocultas, onCerrar = { pantalla = "ajustes" })
            "widgets" -> WidgetsView(activity, widgetHost, store, widgetsGuardados, onCerrar = { pantalla = "ajustes" }) { id -> widgetPendiente = id }
        }

        // Carpeta abierta (overlay)
        AnimatedVisibility(
            visible = carpetaAbierta != null,
            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.85f, animationSpec = tween(200)),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.85f, animationSpec = tween(150))
        ) {
            val carpeta = carpetaAbierta
            if (carpeta != null) {
                VistaCarpeta(
                    carpeta, appsVisibles, ajustes,
                    abrirApp = { repositorio.abrir(it) },
                    quitarApp = { key ->
                        val nueva = carpeta.copy(apps = carpeta.apps - key)
                        scope.launch { store.guardarCarpetas(carpetas.map { if (it.id == carpeta.id) nueva else it }) }
                        carpetaAbierta = nueva
                    },
                    onCerrar = { carpetaAbierta = null }
                )
            }
        }

        if (cargando && pantalla == "home") {
            Text("MiLauncher", color = Tema.moradoClaro, fontSize = 22.sp, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun HomeView(
    activity: Activity, ajustes: Ajustes, apps: List<AppItem>, carpetas: List<Carpeta>,
    abrirCarpeta: (Carpeta) -> Unit, abrirApp: (AppItem) -> Unit,
    onGestoArriba: () -> Unit, onGestoAbajo: () -> Unit, onAjustes: () -> Unit,
    widgetHost: WidgetHostManager, widgetsGuardados: List<WidgetPosicionado>,
    store: SettingsStore, onAnadirWidget: (Int) -> Unit,
    onQuitarWidget: (Int) -> Unit
) {
    var mostrarWidgets by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var mostrarSelectorCarpeta by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // Zona superior: widgets
        if (widgetsGuardados.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                widgetsGuardados.forEach { w ->
                    val vista = remember(w.appWidgetId) { widgetHost.crearVista(w.appWidgetId) }
                    if (vista != null) {
                        Box(Modifier.fillMaxWidth().height((w.spanY * 80).dp)) {
                            AndroidView(factory = { vista }, modifier = Modifier.fillMaxSize())
                            Text(
                                "✕", color = Tema.grisTexto, fontSize = 16.sp,
                                modifier = Modifier.align(Alignment.TopEnd).clickable { onQuitarWidget(w.appWidgetId) }.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Zona central táctil con gestos verticales
        var arrastreAcumulado by remember { mutableStateOf(0f) }
        Box(
            Modifier.weight(1f).fillMaxWidth().pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectVerticalDragGestures(
                    onVerticalDrag = { _, dy -> arrastreAcumulado += dy },
                    onDragEnd = {
                        if (arrastreAcumulado < -60f) onGestoArriba()
                        else if (arrastreAcumulado > 60f) onGestoAbajo()
                        arrastreAcumulado = 0f
                    }
                )
            },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Desliza ↑ cajón · ↓ buscador",
                color = Tema.grisTexto.copy(alpha = 0.5f), fontSize = 13.sp
            )
        }

        // Dock inferior: carpetas + apps fijas primeras
        if (ajustes.mostrarDock) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                carpetas.take(4).forEach { carpeta ->
                    val iconos = carpeta.apps.mapNotNull { key -> apps.find { it.key == key }?.icon }.take(4)
                    Image(
                        bitmap = IconPackLoader.iconoCarpeta(iconos, 128, Tema.moradoOscuro.hashCode()).asImageBitmap(),
                        contentDescription = carpeta.nombre,
                        modifier = Modifier.size(ajustes.tamanoIcono.dp).clickable { abrirCarpeta(carpeta) }
                    )
                }
                IconoBoton("+", "Nueva carpeta") { mostrarSelectorCarpeta = true }
                IconoBoton("⚙", "Ajustes") { onAjustes() }
                IconoBoton("▦", "Widgets") { mostrarWidgets = !mostrarWidgets }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.End) {
                IconoBoton("⚙", "Ajustes") { onAjustes() }
            }
        }
    }

    if (mostrarWidgets) {
        WidgetsView(activity, widgetHost, store, widgetsGuardados, onCerrar = { mostrarWidgets = false }) { id ->
            onAnadirWidget(id)
        }
    }

    if (mostrarSelectorCarpeta) {
        SelectorCrearCarpeta(
            apps, onCancelar = { mostrarSelectorCarpeta = false },
            onCrear = { seleccion, nombre ->
                val nueva = Carpeta(System.currentTimeMillis(), nombre, seleccion.map { it.key })
                scope.launch {
                    val actuales = store.carpetasFlow.first()
                    store.guardarCarpetas(actuales + nueva)
                }
                mostrarSelectorCarpeta = false
            }
        )
    }
}

@Composable
fun IconoBoton(simbolo: String, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
            .background(Tema.moradoOscuro.copy(alpha = 0.7f)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(simbolo, color = Tema.moradoClaro, fontSize = 20.sp)
    }
}

@Composable
fun CajonView(
    activity: Activity, ajustes: Ajustes, apps: List<AppItem>, busqueda: String,
    onBusqueda: (String) -> Unit, abrirApp: (AppItem) -> Unit, onCerrar: () -> Unit,
    onOcultar: (String) -> Unit
) {
    var modoOcultar by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 4 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 4 }
    ) {
        Column(
            Modifier.fillMaxSize()
                .background(Tema.negro.copy(alpha = ajustes.transparenciaCajon))
                .statusBarsPadding().navigationBarsPadding().padding(12.dp)
        ) {
            OutlinedTextField(
                value = busqueda.trim(), onValueChange = onBusqueda,
                placeholder = { Text("Buscar apps…", color = Tema.grisTexto) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Tema.morado, unfocusedBorderColor = Tema.grisOscuro,
                    cursorColor = Tema.morado, focusedTextColor = Tema.blanco, unfocusedTextColor = Tema.blanco
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("↩ Cerrar", color = Tema.moradoClaro, fontSize = 16.sp, modifier = Modifier.clickable { onCerrar() })
                Text(if (modoOcultar) "✓ Hecho" else "👁 Ocultar", color = Tema.grisTexto, fontSize = 14.sp,
                    modifier = Modifier.clickable { modoOcultar = !modoOcultar })
            }
            Text("${apps.size} apps", color = Tema.grisTexto, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(ajustes.columnas),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(ajustes.espaciado.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(apps, key = { it.key }) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (modoOcultar) onOcultar(app.key) else abrirApp(app)
                        }.padding(vertical = ajustes.espaciado.dp)
                    ) {
                        Image(
                            bitmap = app.icon.asImageBitmap(), contentDescription = app.label,
                            modifier = Modifier.size(ajustes.tamanoIcono.dp)
                        )
                        if (ajustes.mostrarNombres) {
                            Spacer(Modifier.height(4.dp))
                            Text(app.label, color = Tema.blanco, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VistaCarpeta(
    carpeta: Carpeta, apps: List<AppItem>, ajustes: Ajustes,
    abrirApp: (AppItem) -> Unit, quitarApp: (String) -> Unit, onCerrar: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { onCerrar() }, contentAlignment = Alignment.Center) {
        var nombre by remember { mutableStateOf(carpeta.nombre) }
        val miembros = carpeta.apps.mapNotNull { key -> apps.find { it.key == key } }
        Column(
            Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(24.dp))
                .background(Tema.grisOscuro).padding(20.dp)
        ) {
            TextField(
                value = nombre, onValueChange = { nombre = it },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Tema.blanco, unfocusedTextColor = Tema.blanco, cursorColor = Tema.morado
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(ajustes.columnas.coerceAtLeast(3)), modifier = Modifier.height(320.dp)) {
                items(miembros, key = { it.key }) { app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { abrirApp(app) }.padding(8.dp)) {
                        Image(bitmap = app.icon.asImageBitmap(), contentDescription = app.label,
                            modifier = Modifier.size(ajustes.tamanoIcono.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(app.label, color = Tema.blanco, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("✕", color = Tema.grisTexto, fontSize = 11.sp,
                            modifier = Modifier.clickable { quitarApp(app.key) })
                    }
                }
            }
            Button(
                onClick = onCerrar,
                colors = ButtonDefaults.buttonColors(containerColor = Tema.morado),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("Cerrar") }
        }
    }
}

@Composable
fun SelectorCrearCarpeta(apps: List<AppItem>, onCancelar: () -> Unit, onCrear: (List<AppItem>, String) -> Unit) {
    var nombre by remember { mutableStateOf("Nueva carpeta") }
    var seleccion by remember { mutableStateOf<Set<String>>(emptySet()) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.92f).clip(RoundedCornerShape(24.dp)).background(Tema.grisOscuro).padding(16.dp)) {
            TextField(value = nombre, onValueChange = { nombre = it },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Tema.blanco, unfocusedTextColor = Tema.blanco, cursorColor = Tema.morado),
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(300.dp)) {
                items(apps, key = { it.key }) { app ->
                    val sel = app.key in seleccion
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            seleccion = if (sel) seleccion - app.key else seleccion + app.key
                        }.padding(6.dp)) {
                        Image(bitmap = app.icon.asImageBitmap(), contentDescription = app.label,
                            modifier = Modifier.size(48.dp).then(
                                if (sel) Modifier.background(Tema.morado, RoundedCornerShape(10.dp)) else Modifier
                            ))
                        Text(app.label, color = if (sel) Tema.moradoClaro else Tema.grisTexto,
                            fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onCancelar) { Text("Cancelar", color = Tema.grisTexto) }
                Button(
                    onClick = { if (seleccion.isNotEmpty()) onCrear(apps.filter { it.key in seleccion }, nombre) },
                    colors = ButtonDefaults.buttonColors(containerColor = Tema.morado),
                    enabled = seleccion.isNotEmpty()
                ) { Text("Crear") }
            }
        }
    }
}

@Composable
fun OcultasView(store: SettingsStore, apps: List<AppItem>, ocultas: Set<String>, onCerrar: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Tema.negro).statusBarsPadding().padding(16.dp)) {
        Text("← Ajustes", color = Tema.moradoClaro, fontSize = 20.sp, modifier = Modifier.clickable { onCerrar() })
        Spacer(Modifier.height(12.dp))
        Text("Aplicaciones ocultas", color = Tema.blanco, fontSize = 22.sp)
        Text("Toca para volver a mostrarlas en el cajón.", color = Tema.grisTexto, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        val lista = apps.filter { it.key in ocultas }.sortedBy { it.label.lowercase() }
        if (lista.isEmpty()) Text("No hay apps ocultas.", color = Tema.grisTexto, fontSize = 14.sp)
        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize()) {
            items(lista, key = { it.key }) { app ->
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { scope.launch { store.toggleOculta(app.key) } }.padding(8.dp)) {
                    Image(bitmap = app.icon.asImageBitmap(), contentDescription = app.label, modifier = Modifier.size(48.dp))
                    Text(app.label, color = Tema.blanco, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun WidgetsView(
    activity: Activity, widgetHost: WidgetHostManager, store: SettingsStore,
    guardados: List<WidgetPosicionado>, onCerrar: () -> Unit, onEnlazado: (Int) -> Unit
) {
    val lista = remember { widgetHost.listarWidgets().sortedBy { it.loadLabel(activity.packageManager).toString() } }
    Column(Modifier.fillMaxSize().background(Tema.negro).statusBarsPadding().padding(16.dp)) {
        Text("← Ajustes", color = Tema.moradoClaro, fontSize = 20.sp, modifier = Modifier.clickable { onCerrar() })
        Spacer(Modifier.height(12.dp))
        Text("Widgets disponibles", color = Tema.blanco, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        val scroll = rememberScrollState()
        Column(Modifier.verticalScroll(scroll).fillMaxSize()) {
            lista.forEach { info ->
                val etiqueta = info.loadLabel(activity.packageManager).toString()
                Row(Modifier.fillMaxWidth().clickable {
                    val id = widgetHost.asignarYEnlazar(info)
                    if (id != null) {
                        if (id > 0) onEnlazado(id)
                        else {
                            // Pedir permiso de bind
                            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -id)
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                            }
                            activity.startActivityForResult(intent, 1001)
                        }
                    }
                }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(etiqueta, color = Tema.blanco, fontSize = 16.sp)
                        Text(info.provider.packageName, color = Tema.grisTexto, fontSize = 12.sp)
                    }
                    Text("+", color = Tema.morado, fontSize = 24.sp)
                }
                HorizontalDivider(color = Tema.grisOscuro)
            }
        }
    }
}

@Composable
fun AjustesView(
    activity: Activity, store: SettingsStore, ajustes: Ajustes,
    apps: List<AppItem>, ocultas: Set<String>, iconPack: String, iconLoader: IconPackLoader,
    onCerrar: () -> Unit, irOcultas: () -> Unit, irWidgets: () -> Unit
) {
    var columnas by remember { mutableStateOf(ajustes.columnas.toFloat()) }
    var tamano by remember { mutableStateOf(ajustes.tamanoIcono.toFloat()) }
    var fondo by remember { mutableStateOf(ajustes.fondoOscuro) }
    var transp by remember { mutableStateOf(ajustes.transparenciaCajon) }
    var espaciado by remember { mutableStateOf(ajustes.espaciado.toFloat()) }
    val packs = remember { iconLoader.listarPacks() }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(Tema.negro).verticalScroll(rememberScrollState())
        .statusBarsPadding().padding(20.dp)) {
        Text("← Ajustes", color = Tema.moradoClaro, fontSize = 22.sp,
            modifier = Modifier.clickable { onCerrar() }.padding(vertical = 8.dp))
        Spacer(Modifier.height(12.dp))

        Seccion("Cuadrícula")
        Text("Columnas: ${columnas.toInt()}", color = Tema.blanco)
        Slider(columnas, { columnas = it }, { scope.launch { store.guardar(ajustes.copy(columnas = columnas.toInt())) } }, 3f..6f, steps = 2)
        Text("Tamaño de iconos: ${tamano.toInt()}dp", color = Tema.blanco)
        Slider(tamano, { tamano = it }, { scope.launch { store.guardar(ajustes.copy(tamanoIcono = tamano.toInt())) } }, 36f..72f)
        Text("Espaciado: ${espaciado.toInt()}dp", color = Tema.blanco)
        Slider(espaciado, { espaciado = it }, { scope.launch { store.guardar(ajustes.copy(espaciado = espaciado.toInt())) } }, 2f..24f)

        Seccion("Apariencia")
        Text("Oscuridad del fondo: ${(fondo * 100).toInt()}%", color = Tema.blanco)
        Slider(fondo, { fondo = it }, { scope.launch { store.guardar(ajustes.copy(fondoOscuro = fondo)) } }, 0f..0.9f)
        Text("Transparencia del cajón: ${(transp * 100).toInt()}%", color = Tema.blanco)
        Slider(transp, { transp = it }, { scope.launch { store.guardar(ajustes.copy(transparenciaCajon = transp)) } }, 0.4f..1f)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Mostrar nombres", color = Tema.blanco, modifier = Modifier.weight(1f))
            Switch(ajustes.mostrarNombres, { scope.launch { store.guardar(ajustes.copy(mostrarNombres = it)) } })
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Mostrar dock", color = Tema.blanco, modifier = Modifier.weight(1f))
            Switch(ajustes.mostrarDock, { scope.launch { store.guardar(ajustes.copy(mostrarDock = it)) } })
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Animaciones", color = Tema.blanco, modifier = Modifier.weight(1f))
            Switch(ajustes.animaciones, { scope.launch { store.guardar(ajustes.copy(animaciones = it)) } })
        }
        Text("Tema", color = Tema.blanco, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("morado", "negro", "azul", "verde").forEach { t ->
                FilterChip(
                    selected = ajustes.tema == t,
                    onClick = { scope.launch { store.guardar(ajustes.copy(tema = t)) } },
                    label = { Text(t.replaceFirstChar { it.uppercase() }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Tema.morado, selectedLabelColor = Color.White,
                        containerColor = Tema.grisOscuro, labelColor = Tema.grisTexto
                    )
                )
            }
        }

        Seccion("Gestos")
        SelectorGesto("Deslizar ↑", ajustes.gestoArriba) { scope.launch { store.guardar(ajustes.copy(gestoArriba = it)) } }
        SelectorGesto("Deslizar ↓", ajustes.gestoAbajo) { scope.launch { store.guardar(ajustes.copy(gestoAbajo = it)) } }
        SelectorGesto("Doble toque", ajustes.gestoDobleToque) { scope.launch { store.guardar(ajustes.copy(gestoDobleToque = it)) } }

        Seccion("Iconos")
        Text("Paquete actual: ${iconPack.ifBlank { "Sistema" }}", color = Tema.grisTexto, fontSize = 13.sp)
        if (packs.isEmpty()) Text("No hay paquetes de iconos instalados.", color = Tema.grisTexto, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            FilterChip(selected = iconPack.isBlank(), onClick = { scope.launch { store.setIconPack("") } },
                label = { Text("Sistema") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Tema.morado, containerColor = Tema.grisOscuro, labelColor = Tema.grisTexto))
            packs.take(3).forEach { pack ->
                FilterChip(selected = iconPack == pack.packageName,
                    onClick = { scope.launch { store.setIconPack(pack.packageName) } },
                    label = { Text(pack.nombre, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Tema.morado, containerColor = Tema.grisOscuro, labelColor = Tema.grisTexto))
            }
        }

        Seccion("Más")
        Text("👁 Aplicaciones ocultas (${ocultas.size})", color = Tema.blanco, fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth().clickable { irOcultas() }.padding(vertical = 10.dp))
        Text("▦ Widgets", color = Tema.blanco, fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth().clickable { irWidgets() }.padding(vertical = 10.dp))
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun Seccion(titulo: String) {
    Text(titulo.uppercase(), color = Tema.morado, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}

@Composable
fun SelectorGesto(etiqueta: String, actual: AccionGesto, onCambiar: (AccionGesto) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth().clickable { expandido = !expandido }.padding(vertical = 8.dp)) {
            Text(etiqueta, color = Tema.blanco, modifier = Modifier.weight(1f))
            Text(actual.etiqueta, color = Tema.moradoClaro)
        }
        if (expandido) {
            AccionGesto.entries.forEach { a ->
                Text(
                    a.etiqueta, color = if (a == actual) Tema.morado else Tema.grisTexto,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().clickable { onCambiar(a); expandido = false }.padding(vertical = 6.dp, horizontal = 12.dp)
                )
            }
        }
    }
}

/** Expande el panel de notificaciones vía StatusBarManager (API oculta, igual que hacen los launchers). */
fun expandirNotificaciones() {
    try {
        val svc = Class.forName("android.app.StatusBarManager")
        val method = svc.getMethod("expandNotificationsPanel")
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, "statusbar")
        val stub = Class.forName("com.android.internal.statusbar.IStatusBarService")
        val asInterface = stub.getMethod("asInterface", android.os.IBinder::class.java)
        method.invoke(asInterface.invoke(null, binder))
    } catch (_: Exception) {
        // En versiones nuevas puede fallar silenciosamente: no es crítico.
    }
}
