plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val appVersion = "0.3.6"

val commitHash = try {
    Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short", "HEAD")).inputStream.reader().use { it.readText().trim() }
} catch (e: Exception) {
    "unknown"
}

val commitDate = try {
    Runtime.getRuntime().exec(arrayOf("git", "log", "-1", "--format=%cd", "--date=format:%d.%m.%Y %H:%M")).inputStream.reader().use { it.readText().trim() }
} catch (e: Exception) {
    "unknown"
}

val versionCodeTimestamp = try {
    // Minutes since January 1st, 2026
    val startTime = 1735686000000L // 2026-01-01 00:00:00 UTC
    ((System.currentTimeMillis() - startTime) / (1000 * 60)).toInt()
} catch (e: Exception) {
    1
}

android {
    namespace = "com.example.familienwecker"


    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.familienwecker"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeTimestamp
        versionName = appVersion

        buildConfigField("String", "COMMIT_HASH", "\"${commitHash}\"")
        buildConfigField("String", "COMMIT_DATE", "\"${commitDate}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xjdk-release=11")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

}

base {
    archivesName.set("FamWake-Familienwecker-v${appVersion}-${commitHash}")
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.fragment.ktx)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation(libs.play.services.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.core.splashscreen)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}