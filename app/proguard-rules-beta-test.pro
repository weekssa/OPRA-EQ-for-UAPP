# Error Prone annotations can reference compiler-only types that are absent on Android.
# Their modifier metadata is not used by the instrumentation runner at runtime.
-dontwarn javax.lang.model.element.Modifier
-keep class androidx.tracing.** { *; }
