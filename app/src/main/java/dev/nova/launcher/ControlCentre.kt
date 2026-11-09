package dev.nova.launcher

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

/** Public APIs only. Connectivity and DND changes stay in Android-owned UI. */
class ControlCentreController(private val activity: MainActivity) {
    private val audio=activity.getSystemService(AudioManager::class.java)
    private val cameras=activity.getSystemService(CameraManager::class.java)
    private val torchId=runCatching { cameras.cameraIdList.firstOrNull { id->cameras.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)==true } }.getOrNull()
    val torchOn=MutableStateFlow(false)
    val torchAvailable=MutableStateFlow(torchId!=null)
    private val torchCallback=object:CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String,enabled: Boolean) { if(cameraId==torchId) { torchAvailable.value=true;torchOn.value=enabled } }
        override fun onTorchModeUnavailable(cameraId: String) { if(cameraId==torchId) { torchAvailable.value=false;torchOn.value=false } }
    }
    private val cameraPermission=activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted) setTorch(true) else activity.model.message.value="Flashlight access was denied. Other controls are still available."
    }
    init { runCatching { cameras.registerTorchCallback(torchCallback,Handler(Looper.getMainLooper())) } }
    fun dispose() { runCatching { cameras.unregisterTorchCallback(torchCallback) } }
    fun toggleTorch() {
        if(torchId==null) return
        if(ContextCompat.checkSelfPermission(activity,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) cameraPermission.launch(Manifest.permission.CAMERA)
        else setTorch(!torchOn.value)
    }
    private fun setTorch(enabled: Boolean) { runCatching { cameras.setTorchMode(requireNotNull(torchId),enabled) }.onFailure { activity.model.message.value="Flashlight is unavailable. Another app may be using the camera." } }
    fun volume(): Int=audio.getStreamVolume(AudioManager.STREAM_MUSIC)
    fun maxVolume(): Int=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    fun fixedVolume(): Boolean=audio.isVolumeFixed
    fun setVolume(value: Int) { runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC,value.coerceIn(0,maxVolume()),0) }.onFailure { activity.model.message.value="Android could not change media volume." } }
    fun brightness(): Float?=runCatching { Settings.System.getInt(activity.contentResolver,Settings.System.SCREEN_BRIGHTNESS).coerceIn(0,255)/255f }.getOrNull()
    fun canWriteBrightness(): Boolean=Settings.System.canWrite(activity)
    fun setBrightness(value: Float) { runCatching {
        check(canWriteBrightness())
        check(Settings.System.putInt(activity.contentResolver,Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL))
        check(Settings.System.putInt(activity.contentResolver,Settings.System.SCREEN_BRIGHTNESS,(value*255).roundToInt().coerceIn(1,255)))
    }.onFailure { activity.model.message.value="Brightness permission is unavailable. Use Android display settings." } }
    fun requestBrightness()=activity.openSystem(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:${activity.packageName}")))
    fun internetLabel(): String {
        val connectivity=activity.getSystemService(ConnectivityManager::class.java)
        val caps=connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return "Not connected"
        val transport=when { caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->"Wi-Fi";caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->"Mobile data";else->"Connected" }
        return if(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) transport else "$transport · no internet"
    }
    fun batteryLabel(): String {
        val manager=activity.getSystemService(BatteryManager::class.java)
        val level=manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if(level !in 0..100) return "Battery status unavailable"
        return "$level%"+if(manager.isCharging) " · charging" else " battery"
    }
    fun internet()=activity.openSystem(Intent(if(Build.VERSION.SDK_INT>=29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIFI_SETTINGS))
    fun bluetooth()=activity.openSystem(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    fun sound()=activity.openSystem(Intent(if(Build.VERSION.SDK_INT>=29) Settings.Panel.ACTION_VOLUME else Settings.ACTION_SOUND_SETTINGS))
    fun display()=activity.openSystem(Intent(Settings.ACTION_DISPLAY_SETTINGS))
    fun focus()=activity.openSystem(Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS))
}

@Composable fun ControlCentre(activity: MainActivity,onBack:()->Unit) {
    val control=activity.controls
    val torchOn by control.torchOn.collectAsState()
    val torchAvailable by control.torchAvailable.collectAsState()
    var internet by remember { mutableStateOf(control.internetLabel()) }
    var battery by remember { mutableStateOf(control.batteryLabel()) }
    var volume by remember { mutableFloatStateOf(control.volume().toFloat()) }
    var brightness by remember { mutableStateOf(control.brightness()) }
    var canWrite by remember { mutableStateOf(control.canWriteBrightness()) }
    var adjusting by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while(true) {
        internet=control.internetLabel();battery=control.batteryLabel();canWrite=control.canWriteBrightness()
        if(!adjusting) {volume=control.volume().toFloat();brightness=control.brightness()}
        delay(2000)
    } }
    Column(Modifier.fillMaxSize().testTag("control-centre")) {
        Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back home") }
            Text("Control centre",Modifier.weight(1f),style=MaterialTheme.typography.headlineMedium)
            IconButton(onClick={activity.openSystem(Intent(Settings.ACTION_SETTINGS))}) { Icon(Icons.Default.Settings,"Android settings") }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            DotMatrixText("NOVA",Modifier.fillMaxWidth(.48f).height(42.dp),MaterialTheme.colorScheme.onSurface)
            Text(battery,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                ControlTile("Internet",internet,Icons.Default.Wifi,Modifier.weight(1f),onClick=control::internet)
                ControlTile("Bluetooth","Devices & pairing",Icons.Default.Bluetooth,Modifier.weight(1f),onClick=control::bluetooth)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                ControlTile("Flashlight",if(!torchAvailable) "Unavailable" else if(torchOn) "On" else "Off",if(torchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,Modifier.weight(1f),active=torchOn,enabled=torchAvailable,onClick=control::toggleTorch)
                ControlTile("Do not disturb","Focus settings",Icons.Default.DoNotDisturbOn,Modifier.weight(1f),onClick=control::focus)
            }
            Surface(shape=MaterialTheme.shapes.extraLarge,color=MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.VolumeUp,null);Spacer(Modifier.width(12.dp));Text("Media volume",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium);Text("${volume.roundToInt()} / ${control.maxVolume()}") }
                    Slider(volume,{adjusting=true;volume=it},enabled=!control.fixedVolume() && control.maxVolume()>0,valueRange=0f..control.maxVolume().coerceAtLeast(1).toFloat(),onValueChangeFinished={control.setVolume(volume.roundToInt());adjusting=false},modifier=Modifier.testTag("media-volume"))
                    TextButton(onClick=control::sound) { Text("All sound controls") }
                }
            }
            Surface(shape=MaterialTheme.shapes.extraLarge,color=MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.Brightness6,null);Spacer(Modifier.width(12.dp));Text("Brightness",style=MaterialTheme.typography.titleMedium) }
                    if(canWrite && brightness!=null) {
                        Slider(brightness!!,{adjusting=true;brightness=it},valueRange=.01f..1f,onValueChangeFinished={control.setBrightness(brightness!!);adjusting=false})
                        Text("Adjusting sets manual brightness.",style=MaterialTheme.typography.bodySmall)
                    } else { Text("Allow NOVA to change brightness, or open Android display settings.",Modifier.padding(top=12.dp),style=MaterialTheme.typography.bodyMedium);TextButton(onClick=control::requestBrightness) { Text("Allow brightness control") } }
                    TextButton(onClick=control::display) { Text("Display settings") }
                }
            }
            Text("Connectivity and focus open Android's own controls.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun ControlTile(title:String,subtitle:String,icon:ImageVector,modifier:Modifier=Modifier,active:Boolean=false,enabled:Boolean=true,onClick:()->Unit) {
    val color=if(active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Surface(onClick=onClick,enabled=enabled,modifier=modifier.heightIn(min=148.dp),shape=MaterialTheme.shapes.extraLarge,color=color,contentColor=if(active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { Icon(icon,null,Modifier.size(28.dp));Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,style=MaterialTheme.typography.bodySmall) }
    }
}
