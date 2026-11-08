package dev.nova.launcher

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.nova.core.GestureAction
import dev.nova.core.Settings

/** Attached only to the inset clock/empty-home area, never the navigation edges or widget surface. */
@Composable fun Modifier.homeGestures(settings: Settings,onAction:(GestureAction)->Unit): Modifier {
    val action=rememberUpdatedState(onAction)
    val threshold=with(LocalDensity.current) { 72.dp.toPx() }
    return this.pointerInput(settings.swipeUp,settings.swipeDown) {
        var distance=0f
        detectVerticalDragGestures(onDragStart={distance=0f},onDragCancel={distance=0f},onDragEnd={
            if(distance < -threshold) action.value(settings.swipeUp) else if(distance>threshold) action.value(settings.swipeDown)
        }) { change,amount -> distance+=amount;change.consume() }
    }.pointerInput(settings.doubleTap) { detectTapGestures(onDoubleTap={action.value(settings.doubleTap)}) }
}
