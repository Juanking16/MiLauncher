package com.milauncher

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap
)

data class Entrada(val letra: String, val app: AppItem?)

val coloresFondo = listOf(
    Color(0xFF000000),
    Color(0xFF0B1B3A),
    Color(0xFF2A0B3A),
    Color(0xFF0B2A1B),
    Color(0xFF2B2B2B)
)
val nombresColor = listOf("Negro", "Azul", "Violeta", "Verde", "Gris")

fun formaIcono(i: Int): Shape = when (i) {
    1 -> CircleShape
    2 -> RoundedCornerShape(14.dp)
    3 -> RoundedCornerShape(4.dp)
    else -> RectangleShape
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HomeScreen() }
    }

    private suspend fun loadApps(): List<AppItem> = withContext(Dispatchers.Default) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map {
                AppItem(
                    label = it.loadLabel(packageManager).toString(),
                    packageName = it.activityInfo.packageName,
                    activityName = it.activityInfo.name,
                    icon = it.loadIcon(packageManager).toBitmap(128, 128).asImageBitmap()
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun openApp(app: AppItem, estilo: Int) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val opciones = when (estilo) {
            1 -> ActivityOptions.makeCustomAnimation(this, R.anim.zoom_in, R.anim.zoom_out)
            2 -> ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
            3 -> ActivityOptions.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
            else -> null
        }
        startActivity(intent, opciones?.toBundle())
    }

    @Composable
    fun HomeScreen() {
        val store = remember { SettingsStore(applicationContext) }
        val ajustes by store.flow.collectAsState(initial = Ajustes())
        val scope = rememberCoroutineScope()
        var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
        var verAjustes by remember { mutableStateOf(false) }
        var busqueda by remember { mutableStateOf("") }
        val filtradas = remember(apps, busqueda) {
            if (busqueda.isBlank()) apps
            else apps.filter { it.label.contains(busqueda, ignoreCase = true) }
        }

        // Entradas para la lista alfabética: encabezado de letra + apps
        val entradas = remember(filtradas) {
            val lista = mutableListOf<Entrada>()
            var ultima = ""
            filtradas.forEach { app ->
                val primera = app.label.firstOrNull()?.uppercaseChar()
                val letra = if (primera != null && primera.isLetter()) primera.toString() else "#"
                if (letra != ultima) {
                    lista.add(Entrada(letra, null))
                    ultima = letra
                }
                lista.add(Entrada(letra, app))
            }
            lista
        }
        val letras = remember(entradas) { entradas.filter { it.app == null }.map { it.letra } }
        val listaEstado = rememberLazyListState()

        val progreso = remember { Animatable(0f) }
        val cajonAbierto = progreso.value > 0.01f
        val alturaPx = with(LocalDensity.current) {
            LocalConfiguration.current.screenHeightDp.dp.toPx()
        }
        val spec: AnimationSpec<Float> = if (ajustes.animCajon == 3)
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        else tween(300)

        fun abrir() { scope.launch { progreso.animateTo(1f, spec) } }
        fun cerrar() { scope.launch { progreso.animateTo(0f, tween(250)) } }
        fun terminarArrastre() {
            scope.launch {
                if (progreso.value > 0.5f) progreso.animateTo(1f, spec)
                else progreso.animateTo(0f, tween(250))
            }
        }

        val columnasEstilo = when (ajustes.estiloCajon) {
            1 -> (ajustes.columnas + 1).coerceAtMost(7)
            2 -> 3
            else -> ajustes.columnas
        }
        val tamanoEstilo = when (ajustes.estiloCajon) {
            1 -> (ajustes.tamanoIcono * 0.8f).toInt()
            2 -> 72
            3, 4 -> 44
            else -> ajustes.tamanoIcono
        }
        val colorBase = coloresFondo[ajustes.colorFondo.coerceIn(0, coloresFondo.size - 1)]
        val forma = formaIcono(ajustes.forma)

        LaunchedEffect(Unit) { apps = loadApps() }
        LaunchedEffect(cajonAbierto) { if (!cajonAbierto) busqueda = "" }
        BackHandler(enabled = verAjustes) { verAjustes = false }
        BackHandler(enabled = cajonAbierto && !verAjustes) { cerrar() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorBase.copy(alpha = ajustes.fondoOscuro))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (verAjustes) {
                PantallaAjustes(
                    ajustes = ajustes,
                    onCambio = { nuevo -> scope.launch { store.guardar(nuevo) } },
                    onCerrar = { verAjustes = false }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(ajustes.animCajon) {
                            detectVerticalDragGestures(
                                onDragEnd = { terminarArrastre() },
                                onDragCancel = { terminarArrastre() }
                            ) { change, dy ->
                                change.consume()
                                scope.launch {
                                    progreso.snapTo(
                                        (progreso.value - dy / alturaPx * 2f).coerceIn(0f, 1f)
                                    )
                                }
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                val p = progreso.value.coerceIn(0f, 1f)
                                alpha = 1f - p
                                scaleX = 1f - 0.08f * p
                                scaleY = 1f - 0.08f * p
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Juan Launcher", color = Color.White, fontSize = 28.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Desliza hacia arriba", color = Color(0xAAFFFFFF), fontSize = 14.sp)
                    }
                    Text(
                        text = "⌃",
                        color = Color.White,
                        fontSize = 30.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clickable { abrir() }
                            .padding(20.dp)
                    )
                    Text(
                        text = "⚙",
                        color = Color.White,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clickable { verAjustes = true }
                            .padding(14.dp)
                    )
                }

                if (cajonAbierto) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .graphicsLayer {
                                val p = progreso.value.coerceIn(0f, 1f)
                                when (ajustes.animCajon) {
                                    1 -> {
                                        scaleX = 0.8f + 0.2f * p
                                        scaleY = 0.8f + 0.2f * p
                                        alpha = p
                                    }
                                    2 -> {
                                        alpha = p
                                    }
                                    else -> {
                                        translationY = (1f - progreso.value.coerceAtMost(1.05f)) * alturaPx
                                        alpha = (p * 2f).coerceIn(0f, 1f)
                                    }
                                }
                            }
                            .background(colorBase.copy(alpha = ajustes.transparenciaCajon))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(ajustes.animCajon) {
                                    detectVerticalDragGestures(
                                        onDragEnd = { terminarArrastre() },
                                        onDragCancel = { terminarArrastre() }
                                    ) { change, dy ->
                                        change.consume()
                                        scope.launch {
                                            progreso.snapTo(
                                                (progreso.value - dy / alturaPx * 2f).coerceIn(0f, 1f)
                                            )
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⌄",
                                color = Color.White,
                                fontSize = 30.sp,
                                modifier = Modifier
                                    .clickable { cerrar() }
                                    .padding(horizontal = 40.dp, vertical = 12.dp)
                            )
                        }
                        BasicTextField(
                            value = busqueda,
                            onValueChange = { busqueda = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0x33FFFFFF))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            decorationBox = { campo ->
                                if (busqueda.isEmpty()) {
                                    Text("Buscar apps…", color = Color(0x99FFFFFF), fontSize = 16.sp)
                                }
                                campo()
                            }
                        )

                        val alAbrir: (AppItem) -> Unit = { app ->
                            scope.launch { progreso.snapTo(0f) }
                            openApp(app, ajustes.animacion)
                        }

                        when (ajustes.estiloCajon) {
                            3 -> {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                    itemsIndexed(filtradas) { indice, app ->
                                        AppFila(app, indice, tamanoEstilo, forma, ajustes.espaciado) {
                                            alAbrir(app)
                                        }
                                    }
                                }
                            }
                            4 -> {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        state = listaEstado,
                                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                                    ) {
                                        itemsIndexed(entradas) { indice, entrada ->
                                            val app = entrada.app
                                            if (app == null) {
                                                Text(
                                                    text = entrada.letra,
                                                    color = Color(0xFFFBBF24),
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.padding(
                                                        start = 16.dp, top = 14.dp, bottom = 4.dp
                                                    )
                                                )
                                            } else {
                                                AppFila(app, indice, tamanoEstilo, forma, ajustes.espaciado) {
                                                    alAbrir(app)
                                                }
                                            }
                                        }
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(28.dp)
                                            .padding(vertical = 8.dp),
                                        verticalArrangement = Arrangement.SpaceEvenly,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        letras.forEach { letra ->
                                            Text(
                                                text = letra,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                modifier = Modifier
                                                    .clickable {
                                                        val pos = entradas.indexOfFirst {
                                                            it.app == null && it.letra == letra
                                                        }
                                                        if (pos >= 0) {
                                                            scope.launch { listaEstado.scrollToItem(pos) }
                                                        }
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(columnasEstilo),
                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                ) {
                                    itemsIndexed(filtradas) { indice, app ->
                                        AppCelda(
                                            app = app,
                                            indice = indice,
                                            tamano = tamanoEstilo,
                                            forma = forma,
                                            espaciado = ajustes.espaciado,
                                            mostrarNombre = ajustes.mostrarNombres
                                        ) { alAbrir(app) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PantallaAjustes(
        ajustes: Ajustes,
        onCambio: (Ajustes) -> Unit,
        onCerrar: () -> Unit
    ) {
        var columnas by remember { mutableStateOf(ajustes.columnas.toFloat()) }
        var tamano by remember { mutableStateOf(ajustes.tamanoIcono.toFloat()) }
        var fondo by remember { mutableStateOf(ajustes.fondoOscuro) }
        var espaciado by remember { mutableStateOf(ajustes.espaciado.toFloat()) }
        var transp by remember { mutableStateOf(ajustes.transparenciaCajon) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "← Ajustes",
                color = Color.White,
                fontSize = 22.sp,
                modifier = Modifier.clickable { onCerrar() }.padding(vertical = 8.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text("Estilo del cajón", color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                listOf("Cuadrícula", "Compacta", "Grande", "Lista", "A-Z").forEachIndexed { i, nombre ->
                    Text(
                        text = nombre,
                        color = if (ajustes.estiloCajon == i) Color(0xFFFBBF24) else Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clickable { onCambio(ajustes.copy(estiloCajon = i)) }
                            .padding(vertical = 8.dp)
                    )
                }
            }
            Text(
                "A-Z agrega letras separadoras e índice lateral.",
                color = Color(0x99FFFFFF),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))
            Text("Forma de iconos", color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf("Original", "Círculo", "Redondeado", "Cuadrado").forEachIndexed { i, nombre ->
                    Text(
                        text = nombre,
                        color = if (ajustes.forma == i) Color(0xFFFBBF24) else Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { onCambio(ajustes.copy(forma = i)) }
                            .padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Color de fondo", color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                nombresColor.forEachIndexed { i, nombre ->
                    Text(
                        text = nombre,
                        color = if (ajustes.colorFondo == i) Color(0xFFFBBF24) else Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { onCambio(ajustes.copy(colorFondo = i)) }
                            .padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
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

            Text("Espaciado entre apps: ${espaciado.toInt()}", color = Color.White)
            Slider(
                value = espaciado,
                onValueChange = { espaciado = it },
                onValueChangeFinished = { onCambio(ajustes.copy(espaciado = espaciado.toInt())) },
                valueRange = 0f..24f
            )

            Text("Opacidad del fondo: ${(fondo * 100).toInt()}%", color = Color.White)
            Slider(
                value = fondo,
                onValueChange = { fondo = it },
                onValueChangeFinished = { onCambio(ajustes.copy(fondoOscuro = fondo)) },
                valueRange = 0f..0.9f
            )

            Text("Transparencia del cajón: ${(transp * 100).toInt()}%", color = Color.White)
            Slider(
                value = transp,
                onValueChange = { transp = it },
                onValueChangeFinished = { onCambio(ajustes.copy(transparenciaCajon = transp)) },
                valueRange = 0.3f..1f
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mostrar nombres", color = Color.White, modifier = Modifier.weight(1f))
                Switch(
                    checked = ajustes.mostrarNombres,
                    onCheckedChange = { onCambio(ajustes.copy(mostrarNombres = it)) }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Animación al abrir apps", color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf("Ninguna", "Zoom", "Fade", "Deslizar").forEachIndexed { i, nombre ->
                    Text(
                        text = nombre,
                        color = if (ajustes.animacion == i) Color(0xFFFBBF24) else Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clickable { onCambio(ajustes.copy(animacion = i)) }
                            .padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Animación del cajón de apps", color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf("Deslizar", "Zoom", "Fade", "Rebote").forEachIndexed { i, nombre ->
                    Text(
                        text = nombre,
                        color = if (ajustes.animCajon == i) Color(0xFFFBBF24) else Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clickable { onCambio(ajustes.copy(animCajon = i)) }
                            .padding(vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun aparicionAnimada(indice: Int): Float {
    val a = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        delay(minOf(indice, 12) * 25L)
        a.animateTo(
            1f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    return a.value
}

@Composable
fun AppCelda(
    app: AppItem,
    indice: Int,
    tamano: Int,
    forma: Shape,
    espaciado: Int,
    mostrarNombre: Boolean,
    onClick: () -> Unit
) {
    val escala = aparicionAnimada(indice)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
                alpha = ((escala - 0.5f) * 2f).coerceIn(0f, 1f)
            }
            .clickable { onClick() }
            .padding(vertical = espaciado.dp)
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(tamano.dp).clip(forma)
        )
        if (mostrarNombre) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AppFila(
    app: AppItem,
    indice: Int,
    tamano: Int,
    forma: Shape,
    espaciado: Int,
    onClick: () -> Unit
) {
    val escala = aparicionAnimada(indice)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = ((escala - 0.5f) * 2f).coerceIn(0f, 1f)
                translationX = (1f - escala) * 80f
            }
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = (espaciado / 2 + 2).dp)
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(tamano.dp).clip(forma)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}