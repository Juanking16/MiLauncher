package com.milauncher

import android.app.Activity
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Todo lo que necesita la Activity: abrir, desinstalar, info y paquetes de iconos
class Acciones(private val act: Activity) {

    fun clavesApps(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return act.packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != act.packageName }
            .map { it.activityInfo.packageName + "/" + it.activityInfo.name }
            .toSet()
    }

    suspend fun cargarApps(): List<AppItem> = withContext(Dispatchers.Default) {
        val pm = act.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != act.packageName }
            .map {
                val flags = it.activityInfo.applicationInfo.flags
                val esSistema = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                AppItem(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    activityName = it.activityInfo.name,
                    icon = it.loadIcon(pm).toBitmap(128, 128).asImageBitmap(),
                    sistema = esSistema
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun aplicarPack(base: List<AppItem>, pack: String): List<AppItem> =
        withContext(Dispatchers.Default) {
            IconPacks(act.applicationContext).aplicar(base, pack)
        }

    fun detectarPacks(): List<PaqueteIconos> = IconPacks(act.applicationContext).detectar()

    fun openApp(app: AppItem, estilo: Int, origen: Rect? = null) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val vista = act.window.decorView
        val x = origen?.left?.toInt() ?: (vista.width / 2)
        val y = origen?.top?.toInt() ?: (vista.height / 2)
        val ancho = origen?.width?.toInt() ?: 1
        val alto = origen?.height?.toInt() ?: 1

        val opciones = when (estilo) {
            1 -> ActivityOptions.makeCustomAnimation(act, R.anim.zoom_in, R.anim.zoom_out)
            2 -> ActivityOptions.makeCustomAnimation(act, R.anim.fade_in, R.anim.fade_out)
            3 -> ActivityOptions.makeCustomAnimation(act, R.anim.slide_in, R.anim.slide_out)
            4 -> ActivityOptions.makeScaleUpAnimation(vista, x, y, ancho, alto)
            5 -> ActivityOptions.makeClipRevealAnimation(vista, x, y, ancho, alto)
            6 -> ActivityOptions.makeCustomAnimation(act, R.anim.slide_up_in, R.anim.slide_up_out)
            7 -> ActivityOptions.makeCustomAnimation(act, R.anim.bounce_in, R.anim.bounce_out)
            8 -> ActivityOptions.makeCustomAnimation(act, R.anim.spin_in, R.anim.spin_out)
            else -> null
        }
        act.startActivity(intent, opciones?.toBundle())
    }

    fun desinstalar(app: AppItem) {
        try {
            val i = Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.packageName))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            act.startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(act, "No se pudo abrir el desinstalador", Toast.LENGTH_SHORT).show()
        }
    }

    fun infoApp(app: AppItem) {
        try {
            val i = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + app.packageName)
            )
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            act.startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(act, "No se pudo abrir la información", Toast.LENGTH_SHORT).show()
        }
    }

    fun buscarPacks() {
        try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=icon%20pack&c=apps"))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            act.startActivity(i)
        } catch (e: Exception) {
            try {
                val i = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/search?q=icon%20pack&c=apps")
                )
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                act.startActivity(i)
            } catch (e2: Exception) {
                Toast.makeText(act, "No se pudo abrir la tienda", Toast.LENGTH_SHORT).show()
            }
        }
    }
}