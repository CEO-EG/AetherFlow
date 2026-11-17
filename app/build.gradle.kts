plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ceoeg.aetherflow"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ceoeg.aetherflow"
        minSdk = 24
        targetSdk = 36
        versionCode = 100
        versionName = "1.0.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

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
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:parser"))
    implementation(project(":core:routing"))
    implementation(project(":core:tun"))
    implementation(project(":core:engine-xray"))
    implementation(project(":core:engine-singbox"))
    implementation(project(":core:engine-wireguard"))
    implementation(project(":core:vpn"))

    // Package native gomobile / JNI AARs directly into the application APK (AGP 9 compliant)
    implementation(fileTree(mapOf("dir" to "../core/engine-xray/libs", "include" to listOf("*.aar", "*.jar"))))
    implementation(fileTree(mapOf("dir" to "../core/engine-singbox/libs", "include" to listOf("*.aar", "*.jar"))))
    implementation(fileTree(mapOf("dir" to "../core/engine-wireguard/libs", "include" to listOf("*.aar", "*.jar"))))

    implementation(project(":feature:dashboard"))
    implementation(project(":feature:profiles"))
    implementation(project(":feature:subscriptions"))
    implementation(project(":feature:routing"))
    implementation(project(":feature:diagnostics"))
    implementation(project(":feature:settings"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
