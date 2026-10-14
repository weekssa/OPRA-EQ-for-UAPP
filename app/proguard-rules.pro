# AndroidX Test's instrumentation runner calls the platform-independent tracing
# facade from its test process. Keep the facade in the tested app package so
# the runner can resolve its shared runtime dependency on API 26.
-keep class androidx.tracing.** { *; }
# AndroidX Test's storage setup uses Kotlin's lazy facade from the shared
# instrumentation classpath on API 26.
-keep class kotlin.LazyKt { *; }
