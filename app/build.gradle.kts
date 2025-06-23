import org.gradle.api.internal.provider.Providers.changing

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.aa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.aa"
        minSdk = 31
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose.android)
    implementation(libs.androidx.camera.core)
    implementation(libs.litert)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("androidx.compose.material3:material3-android:1.4.0-alpha15")
    implementation("androidx.compose.material:material-icons-core:1.6.8")

    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    implementation("com.google.android.gms:play-services-base:18.6.0")

    // To use CallbackToFutureAdapter
    implementation("androidx.concurrent:concurrent-futures:1.2.0")
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.6.0")

//    runtimeOnly("org.tensorflow:tensorflow-lite:2.17.0")

    implementation("com.google.ai.edge.litert:litert:1.3.0")
    implementation("com.google.ai.edge.litert:litert-support-api:1.3.0")
    implementation("com.google.ai.edge.litert:litert-metadata:1.3.0")

//    runtimeOnly("com.google.ai.edge.litert:litert:2.0.1-alpha")
//    runtimeOnly("com.google.ai.edge.litert.support:litert:2.0.1-alpha")
//    runtimeOnly("com.google.ai.edge.litert.metadata:litert:2.0.1-alpha")
}