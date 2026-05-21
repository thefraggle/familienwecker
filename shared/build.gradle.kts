import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    
    val xcf = XCFramework("shared")
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
            
            // Database
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            
            // Settings
            api(libs.multiplatform.settings)
            api(libs.multiplatform.settings.serialization)

            // Firebase KMP (GitLive)
            api(libs.firebase.gitlive.firestore)
            api(libs.firebase.gitlive.functions)
            api(libs.firebase.gitlive.auth)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.androidx.security.crypto)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.androidx.datastore.preferences)
        }
        iosMain.dependencies {
        }
    }
}

room {
    schemaDirectory("schemas")
}

dependencies {
    // Firebase BOM: Versionsmanagement für transitive Google Firebase Abhängigkeiten
    add("androidMainImplementation", platform(libs.firebase.bom))
    // GitLive 2.1.0 erwartet -ktx Artefakte (nicht in der BOM 34.x enthalten)
    add("androidMainImplementation", libs.firebase.auth.ktx)
    add("androidMainImplementation", libs.firebase.common.ktx)
    // Room Compiler needs to be added via ksp for the specific targets
    add("kspCommonMainMetadata", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

android {
    namespace = "de.familienwecker.famwake.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
