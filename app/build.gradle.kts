import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Configuración local NO versionada (local.properties). Ver docs/ANDROID_RUN.md.
//   farmealotu.apiBaseUrl=http://10.0.2.2:8000/        (debug: emulador -> PC anfitrión)
//   farmealotu.apiBaseUrlRelease=https://<backend>/    (release / demo pública: solo HTTPS)
//   farmealotu.demoMode=false                          (true = arranca con fixtures rotulados DEMO)
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProp(key: String, default: String): String =
    (localProps.getProperty(key) ?: default).trim()

fun asBaseUrl(raw: String): String = if (raw.endsWith("/")) raw else "$raw/"

android {
    namespace = "com.example.farmealotu"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.farmealotu"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "boolean",
            "DEMO_MODE_DEFAULT",
            localProp("farmealotu.demoMode", "false").toBoolean().toString()
        )
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${asBaseUrl(localProp("farmealotu.apiBaseUrl", "http://10.0.2.2:8000/"))}\""
            )
        }
        release {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${asBaseUrl(localProp("farmealotu.apiBaseUrlRelease", "https://farmealotu.invalid/"))}\""
            )
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Recursos y manifiesto disponibles para las pruebas de UI con Robolectric (JVM, sin emulador).
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.osmdroid.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
