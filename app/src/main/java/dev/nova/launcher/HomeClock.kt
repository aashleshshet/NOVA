package dev.nova.launcher

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Date

@Composable fun HomeClock() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while(true) { now=System.currentTimeMillis(); delay(60_000L-now%60_000L) } }
    val context=LocalContext.current
    Column(Modifier.fillMaxWidth().padding(horizontal=28.dp,vertical=16.dp)) {
        Text(DateFormat.getTimeFormat(context).format(Date(now)),style=MaterialTheme.typography.displayLarge.copy(fontSize=76.sp,lineHeight=80.sp),maxLines=1)
        Text(java.text.SimpleDateFormat("EEEE, MMMM d",java.util.Locale.getDefault()).format(Date(now)),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
