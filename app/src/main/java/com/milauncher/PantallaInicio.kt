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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PantallaInicio(
    acciones: Acciones, 
    resumeTick: Int,
    viewModel: HomeViewModel = viewModel() // Inyectamos el ViewModel
) {
    val ctx = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var pantalla by remember { mutableStateOf(0) }
    var menuApp by remember { mutableStateOf<AppItem?>(null) }
    var iconoPropio by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    // Efecto para reaccionar a cuando el usuario vuelve a la app (instala/desinstala)
    LaunchedEffect(resumeTick) {
        viewModel.verificarCambios(resumeTick)
    }

    // Cargar icono propio en segundo plano
    LaunchedEffect(Unit) {
        iconoPropio = withContext(Dispatchers.IO) {
            ctx.packageManager.getApplicationIcon(ctx.packageName).toBitmap(128, 128).asImageBitmap()
        }
    }

    // Navegación
    val columnasPagina = uiState.ajustes.columnas.coerceAtLeast(3)
    val filasPagina = 5
    val porPagina = columnasPagina * filasPagina
    val paginasApps = if (uiState.apps.isEmpty()) 0 else (uiState.apps.size + porPagina - 1) / porPagina
    val totalPaginas = 1 + paginasApps
    val pagerState = rememberPagerState(pageCount = { totalPaginas })

    val colorBase = coloresFondo[uiState.ajustes.colorFondo.coerceIn(0, coloresFondo.size - 1)]
    val forma = formaIcono(uiState.ajustes.forma)
    
    val alineacionReloj = when (uiState.ajustes.relojPosicion) {
        1 -> Alignment.TopCenter
        2 -> Alignment.TopEnd
        3 -> Alignment.Center
        else -> Alignment.TopStart
    }
    val fondoVista = if (pantalla != 0 || uiState.ajustes.fondoModo == 1) Paleta.FondoAmoled
    else colorBase.copy(alpha = uiState.ajustes.fondoOscuro)

    fun aviso(texto: String) { Toast.makeText(ctx, texto, Toast.LENGTH_SHORT).show() }

    // Gestión de botones Atrás
    BackHandler(enabled = pantalla != 0) { pantalla = atrasDe(pantalla) }
    BackHandler(enabled = pantalla == 0 && pagerState.currentPage != 0) { 
        scope.launch { pagerState.animateScrollToPage(0) } 
    }
    BackHandler(enabled = menuApp != null) { menuApp = null }

    TemaLauncher(uiState.ajustes.acento) {
        Box(modifier = Modifier.fillMaxSize().background(fondoVista).statusBarsPadding().navigationBarsPadding()) {
            if (pantalla != 0) {
                PantallaAjustes(
                    pantalla = pantalla, 
                    ajustes = uiState.ajustes, 
                    packs = uiState.packs,
                    onCambio = { nuevo -> viewModel.actualizarAjustes(nuevo) },
                    onIr = { pantalla = it }, 
                    onBuscarPacks = { acciones.buscarPacks() }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { pagina ->
                        Box(modifier = Modifier.fillMaxSize().efectoPagina(pagerState, pagina, uiState.ajustes.transPagina)) {
                            if (pagina == 0) {
                                if (uiState.ajustes.mostrarReloj) {
                                    Reloj(ajustes = uiState.ajustes, modifier = Modifier.align(alineacionReloj).padding(horizontal = 28.dp, vertical = 36.dp))
                                }
                                
                                val elementos = buildList {
                                    iconoPropio?.let { icono ->
                                        add(Elemento(Atajo("Juan Launcher", icono) { pantalla = 1 }, null))
                                    }
                                    uiState.inicioApps.forEach { app ->
                                        add(Elemento(Atajo(app.label, app.icon) { r -> acciones.openApp(app, uiState.ajustes.animacion, r) }, app))
                                    }
                                }
                                
                                Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp)) {
                                    elementos.chunked(4).forEach { fila ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                            fila.forEach { el ->
                                                IconoAtajo(
                                                    atajo = el.atajo, tamano = 58, forma = forma, mostrarNombre = true, 
                                                    modifier = Modifier.weight(1f), 
                                                    alMantener = if (el.app != null) { { menuApp = el.app } } else null
                                                )
                                            }
                                            repeat(4 - fila.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            } else {
                                val desde = (pagina - 1) * porPagina
                                PaginaApps(
                                    lista = uiState.apps.drop(desde).take(porPagina),
                                    columnas = columnasPagina, filas = filasPagina,
                                    tamano = uiState.ajustes.tamanoIcono, forma = forma, mostrarNombre = uiState.ajustes.mostrarNombres,
                                    onAbrir = { app, r -> acciones.openApp(app, uiState.ajustes.animacion, r) },
                                    onMantener = { app -> menuApp = app }
                                )
                            }
                        }
                    }

                    Puntos(total = totalPaginas, actual = pagerState.currentPage)

                    if (uiState.dockApps.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(30.dp)).background(Color(0x33FFFFFF)).padding(vertical = 10.dp)) {
                            uiState.dockApps.forEach { app ->
                                IconoAtajo(
                                    atajo = Atajo(app.label, app.icon) { r -> acciones.openApp(app, uiState.ajustes.animacion, r) },
                                    tamano = 56, forma = forma, mostrarNombre = false, modifier = Modifier.weight(1f), 
                                    alMantener = { menuApp = app }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Menú contextual
                val sel = menuApp
                if (sel != null) {
                    val enInicio = uiState.inicioApps.any { it.packageName == sel.packageName }
                    val enDock = uiState.dockApps.any { it.packageName == sel.packageName }
                    
                    val opciones = buildList<Opcion> {
                        if (!enInicio) add(Opcion(if (enDock) "Mover al inicio" else "Agregar al inicio") { 
                            viewModel.agregarAlInicio(sel)
                            menuApp = null 
                        })
                        if (!enDock) add(Opcion(if (enInicio) "Mover al dock" else "Agregar al dock") { 
                            viewModel.agregarAlDock(sel)
                            menuApp = null 
                        })
                        if (enInicio || enDock) add(Opcion(if (enDock) "Quitar del dock" else "Quitar del inicio") { 
                            viewModel.quitarApp(sel)
                            menuApp = null 
                        })
                        add(Opcion("Información de la app") { acciones.infoApp(sel); menuApp = null })
                        if (!sel.sistema) add(Opcion("Desinstalar", true) { acciones.desinstalar(sel); menuApp = null })
                    }
                    MenuApp(app = sel, opciones = opciones, onCerrar = { menuApp = null })
                }

                // Aviso de nuevo pack
                val nuevoPack = uiState.packNuevo
                if (nuevoPack != null && menuApp == null) {
                    AvisoPack(
                        nombre = nuevoPack.nombre,
                        onAplicar = { viewModel.resolverPackNuevo(true) },
                        onCerrar = { viewModel.resolverPackNuevo(false) }
                    )
                }
            }
        }
    }
}

// PaginaApps se queda exactamente igual que antes.
// ...