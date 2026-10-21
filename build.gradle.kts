buildscript {
    configurations.classpath {
        // AGP 9.2.0 brings these vulnerable transitive versions onto the build-tool classpath.
        resolutionStrategy.force(
            "org.bouncycastle:bcprov-jdk18on:1.85",
            "org.bouncycastle:bcpkix-jdk18on:1.85",
            "org.bouncycastle:bcutil-jdk18on:1.85",
            "org.apache.commons:commons-lang3:3.18.0",
            "org.bitbucket.b_c:jose4j:0.9.6",
            "org.jdom:jdom2:2.0.6.1",
        )
    }
}

plugins {
    id("com.android.application") version "9.2.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
}

subprojects {
    configurations.configureEach {
        if (
            name == "androidLintTool" ||
            name == "unified-test-platform-android-test-plugin-result-listener-gradle"
        ) {
            // These AGP-generated tool configurations resolve SDK tooling independently of the
            // root buildscript classpath. Keep their transitive selections on the patched releases.
            resolutionStrategy.force(
                "org.apache.httpcomponents:httpclient:4.5.14",
                "org.apache.httpcomponents:httpmime:4.5.14",
                "org.apache.commons:commons-lang3:3.18.0",
                "org.bouncycastle:bcprov-jdk18on:1.85",
                "org.bouncycastle:bcpkix-jdk18on:1.85",
                "org.bouncycastle:bcutil-jdk18on:1.85",
            )
        }
        if (name.startsWith("unified-test-platform-")) {
            // Keep the AGP-created UTP configuration on one patched Netty release family.
            resolutionStrategy.force(
                "io.netty:netty-buffer:4.1.137.Final",
                "io.netty:netty-codec:4.1.137.Final",
                "io.netty:netty-codec-http:4.1.137.Final",
                "io.netty:netty-codec-http2:4.1.137.Final",
                "io.netty:netty-codec-socks:4.1.137.Final",
                "io.netty:netty-common:4.1.137.Final",
                "io.netty:netty-handler:4.1.137.Final",
                "io.netty:netty-handler-proxy:4.1.137.Final",
                "io.netty:netty-resolver:4.1.137.Final",
                "io.netty:netty-transport:4.1.137.Final",
                "io.netty:netty-transport-native-unix-common:4.1.137.Final",
            )
        }
        if (name.contains("AndroidTest")) {
            // These two alerts resolve only in Android instrumentation-test configurations.
            resolutionStrategy.force(
                "com.google.guava:guava:32.0.0-android",
                "org.jsoup:jsoup:1.23.2",
            )
        }
    }
}
