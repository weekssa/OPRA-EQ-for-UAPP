# Error Prone annotations can reference compiler-only types that are absent on Android.
# Their modifier metadata is not used by the instrumentation runner at runtime.
-dontwarn javax.lang.model.element.Modifier
# Guava reflection (via Truth in the instrumentation test APK) references a JDK-only
# annotation type that Android does not ship. It is never loaded at runtime.
-dontwarn java.lang.reflect.AnnotatedType
-keep class androidx.tracing.** { *; }
