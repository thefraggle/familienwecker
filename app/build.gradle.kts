import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// Fallback version if no versionName property is provided (e.g. for local builds)
val appVersion = project.findProperty("versionName")?.toString() ?: "1.6.16"

val commitHash = providers.gradleProperty("commitHash").getOrElse("dev")
val commitDate = providers.gradleProperty("commitDate").getOrElse("dev")

val versionCodeTimestamp = try {
    // Minutes since January 1st, 2025
    val startTime = 1735686000000L // 2025-01-01 00:00:00 UTC (roughly)
    ((System.currentTimeMillis() - startTime) / (1000 * 60)).toInt()
} catch (e: Exception) {
    1
}

android {
    namespace = "de.familienwecker.famwake"


    compileSdk = 36

    defaultConfig {
        applicationId = "de.familienwecker.famwake"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeTimestamp
        versionName = appVersion

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val googleClientId = localProperties.getProperty("DEFAULT_WEB_CLIENT_ID") ?: ""
        val localRevenueCatKey = localProperties.getProperty("REVENUECAT_PUBLIC_API_KEY")
        val revenueCatKey = localRevenueCatKey ?: System.getenv("REVENUECAT_PUBLIC_API_KEY") ?: ""
        
        // Only add resValue if it's not already provided by google-services.json
        val googleServicesFile = rootProject.file("app/google-services.json")
        if (!googleServicesFile.exists() && googleClientId.isNotEmpty()) {
            resValue("string", "default_web_client_id", googleClientId)
        }

        buildConfigField("String", "COMMIT_HASH", "\"${commitHash}\"")
        buildConfigField("String", "COMMIT_DATE", "\"${commitDate}\"")
        buildConfigField("String", "REVENUECAT_PUBLIC_API_KEY", "\"${revenueCatKey}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

}

base {
    archivesName.set("FamWake-Familienwecker-v${appVersion}-${commitHash}")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.appcompat)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.common.ktx)
    implementation(libs.firebase.functions)
    implementation(libs.kotlinx.coroutines.play.services)
    // GitLive Auth für FirebaseUser und GoogleAuthProvider.credential()
    implementation(libs.firebase.gitlive.auth)
    implementation(libs.firebase.gitlive.firestore)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Lottie
    implementation(libs.lottie.compose)

    // Play In-App Review
    implementation(libs.play.review.ktx)

    // RevenueCat
    implementation(libs.revenuecat.purchases)

    // Room & SQLite
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.bundled)
}