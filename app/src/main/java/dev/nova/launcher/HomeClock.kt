package dev.nova.launcher

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

private val glyphs=mapOf(
    '0' to "01110/10001/10011/10101/11001/10001/01110", '1' to "00100/01100/00100/00100/00100/00100/01110",
    '2' to "01110/10001/00001/00010/00100/01000/11111", '3' to "11110/00001/00001/01110/00001/00001/11110",
    '4' to "00010/00110/01010/10010/11111/00010/00010", '5' to "11111/10000/10000/11110/00001/00001/11110",
    '6' to "01110/10000/10000/11110/10001/10001/01110", '7' to "11111/00001/00010/00100/01000/01000/01000",
    '8' to "01110/10001/10001/01110/10001/10001/01110", '9' to "01110/10001/10001/01111/00001/00001/01110",
    ':' to "0/1/1/0/1/1/0", 'N' to "10001/11001/11001/10101/10011/10011/10001",
    'O' to "01110/10001/10001/10001/10001/10001/01110", 'V' to "10001/10001/10001/10001/10001/01010/00100",
    'A' to "01110/10001/10001/11111/10001/10001/10001"
)
@Composable fun DotMatrixText(text:String,modifier:Modifier,color:Color) {
    val patterns=remember(text) { text.mapNotNull { glyphs[it]?.split('/') } }
    Canvas(modifier.semantics { contentDescription=text }) {
        val columns=patterns.sumOf { it.first().length+1 }.coerceAtLeast(1)-1
        val unit=minOf(size.width/columns.coerceAtLeast(1),size.height/7)
        var x=0f
        patterns.forEach { rows -> rows.forEachIndexed { row,line -> line.forEachIndexed { col,dot -> if(dot=='1') drawCircle(color,unit*.34f,Offset(x+(col+.5f)*unit,(row+.5f)*unit)) } };x+=(rows.first().length+1)*unit }
    }
}
@Composable fun HomeClock() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while(true) { now=System.currentTimeMillis();delay(60_000L-now%60_000L) } }
    val context=LocalContext.current
    val date=Date(now)
    val is24=DateFormat.is24HourFormat(context)
    val clock=java.text.SimpleDateFormat(if(is24) "HH:mm" else "h:mm",Locale.US).format(date)
    Column(Modifier.fillMaxWidth().padding(vertical=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        DotMatrixText(clock,Modifier.fillMaxWidth().height(100.dp),MaterialTheme.colorScheme.onBackground)
        Text(java.text.SimpleDateFormat(if(is24) "EEE, dd MMMM" else "EEE, dd MMMM · a",Locale.getDefault()).format(date),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
