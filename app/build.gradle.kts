plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

val opraCatalogUrl = providers.gradleProperty("OPRA_CATALOG_URL")
    .orElse("https://opra.roonlabs.net/database_v1.jsonl")
val canonicalCatalogUrl = providers.gradleProperty("CANONICAL_CATALOG_URL")
    .orElse("https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/catalog-live/catalog/catalog.json")
val latestReleaseApiUrl = providers.gradleProperty("LATEST_RELEASE_API_URL")
    .orElse("https://api.github.com/repos/weekssa/OPRA-EQ-for-UAPP/releases/latest")

android {
    namespace = "com.weekssa.opraeqforuapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.weekssa.opraeqforuapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "0.5.0"

        buildConfigField("String", "OPRA_CATALOG_URL", "\"${opraCatalogUrl.get()}\"")
        buildConfigField("String", "CANONICAL_CATALOG_URL", "\"${canonicalCatalogUrl.get()}\"")
        buildConfigField("String", "LATEST_RELEASE_API_URL", "\"${latestReleaseApiUrl.get()}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    val lifecycleVersion = "2.10.0"
    val roomVersion = "2.8.4"
    val workVersion = "2.11.2"

    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.work:work-runtime-ktx:$workVersion")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.4.4")
}
