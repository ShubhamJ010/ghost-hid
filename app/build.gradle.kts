import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val signingPropsFile: File = rootProject.file("keystore.properties")
val signingProps = Properties()
if (signingPropsFile.exists()) {
    FileInputStream(signingPropsFile).use { fis ->
        signingProps.load(fis)
    }
}

fun signingProp(key: String, envVar: String): String {
    val propVal = signingProps.getProperty(key)
    if (!propVal.isNullOrBlank()) return propVal
    return System.getenv(envVar) ?: ""
}

val storeFilePath = signingProp("storeFile", "KEYSTORE_FILE").ifBlank { "release.jks" }
val resolvedStoreFile: File? = run {
    val f = File(storeFilePath)
    val candidate = if (f.isAbsolute) f else rootProject.file(storeFilePath)
    if (candidate.exists()) candidate else null
}

val storePasswordVal = signingProp("storePassword", "KEYSTORE_PASSWORD")
val keyAliasVal = signingProp("keyAlias", "KEY_ALIAS")
val keyPasswordVal = signingProp("keyPassword", "KEY_PASSWORD")

val hasReleaseSigning = resolvedStoreFile != null &&
        storePasswordVal.isNotBlank() &&
        keyAliasVal.isNotBlank() &&
        keyPasswordVal.isNotBlank()

android {
    namespace = "com.sj010.ghid"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sj010.ghid"
        minSdk = 28
        targetSdk = 35
        versionCode = 7
        versionName = "0.1.1"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            if (hasReleaseSigning) {
                storeFile = resolvedStoreFile
                storePassword = storePasswordVal
                keyAlias = keyAliasVal
                keyPassword = keyPasswordVal
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                null
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ""
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

gradle.taskGraph.whenReady {
    val hasReleaseTask = allTasks.any { it.name.contains("Release", ignoreCase = true) }
    if (hasReleaseTask && !hasReleaseSigning) {
        throw GradleException(
            "Release signing configuration missing! " +
            "Please create keystore.properties (see keystore.properties.example) " +
            "or set environment variables: KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD. " +
            "Release builds cannot fall back to debug.keystore."
        )
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-beta02")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.graphics:graphics-shapes:1.0.1")

    // Architecture & Coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Security & EncryptedSharedPreferences (Hardware Keystore backed)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
