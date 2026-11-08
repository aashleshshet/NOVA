package dev.nova.launcher;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

/** A real RemoteViews provider installed only in the instrumentation APK. */
public class FixtureWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) manager.updateAppWidget(id, new RemoteViews(context.getPackageName(), dev.nova.launcher.test.R.layout.test_widget));
    }
}
