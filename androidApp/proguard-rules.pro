# Glance makes the widgets' tap callbacks by class name, so R8 can't see them used.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
