package dev.nova.launcher

import android.app.Application

class NovaApplication : Application() {
    val store by lazy { StateStore(this) }
}
