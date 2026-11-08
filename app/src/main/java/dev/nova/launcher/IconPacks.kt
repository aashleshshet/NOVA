package dev.nova.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Xml
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

data class IconPack(val packageName: String,val label: String)
class IconPacks(private val context: Context) {
    private var loadedPackage=""
    private var mapping: Map<String,String> = emptyMap()
    private val pm=context.packageManager
    suspend fun discover(): List<IconPack> = withContext(Dispatchers.IO) {
        listOf("org.adw.launcher.THEMES","com.novalauncher.THEME").flatMap { action->pm.queryIntentActivities(Intent(action),0) }
            .distinctBy { it.activityInfo.packageName }.map { IconPack(it.activityInfo.packageName,it.loadLabel(pm).toString()) }.sortedBy { it.label.lowercase() }
    }
    @Synchronized fun clear() { loadedPackage="";mapping=emptyMap() }
    @Synchronized fun icon(packageName: String,component: String): Bitmap? {
        if(packageName.isBlank()) return null
        return runCatching {
            val resources=pm.getResourcesForApplication(packageName)
            if(loadedPackage!=packageName) {
                val xmlId=resources.getIdentifier("appfilter","xml",packageName)
                mapping=if(xmlId!=0) resources.getXml(xmlId).use(::parse) else resources.assets.open("appfilter.xml").use { input ->
                    val limited=input.readBytesLimited(2*1024*1024)
                    val parser=Xml.newPullParser(); parser.setInput(limited.inputStream(),"UTF-8");parse(parser)
                }
                loadedPackage=packageName
            }
            val name=mapping[component] ?: return null
            val id=resources.getIdentifier(name,"drawable",packageName)
            if(id==0) null else ResourcesCompat.getDrawable(resources,id,null)?.toBitmap(144,144)
        }.getOrNull()
    }
    companion object {
        fun parse(parser: XmlPullParser): Map<String,String> {
            val result=mutableMapOf<String,String>(); var events=0
            while(parser.eventType!=XmlPullParser.END_DOCUMENT) {
                require(++events<=200_000) { "Icon pack metadata is too large" }
                if(parser.eventType==XmlPullParser.START_TAG && parser.name=="item") {
                    val raw=parser.getAttributeValue(null,"component").orEmpty().removePrefix("ComponentInfo{").removeSuffix("}")
                    val component=ComponentName.unflattenFromString(raw)?.flattenToString()
                    val drawable=parser.getAttributeValue(null,"drawable").orEmpty()
                    if(component!=null && drawable.matches(Regex("[a-zA-Z0-9_]+")) && result.size<40_000) result[component]=drawable
                }
                parser.next()
            }
            return result
        }
    }
}
private fun java.io.InputStream.readBytesLimited(max: Int): ByteArray {
    val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
    while(true) { val n=read(buffer);if(n<0) break;require(out.size()+n<=max);out.write(buffer,0,n) };return out.toByteArray()
}
