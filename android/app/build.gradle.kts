import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { load(it) }
    }
}

// Read simple KEY=value entries from the Android-local environment file.
val environmentProperties = Properties().apply {
    val environmentFile = rootProject.file(".env")
    if (environmentFile.exists()) {
        environmentFile.inputStream().use { load(it) }
    }
}

val apiBaseUrl = (providers.gradleProperty("apiBaseUrl").orNull
    ?: System.getenv("API_BASE_URL")
    ?: environmentProperties.getProperty("API_BASE_URL")
    ?: "http://10.0.2.2:5224/api/").trim().trimEnd('/') + "/"
require(apiBaseUrl.startsWith("http://") || apiBaseUrl.startsWith("https://")) {
    "API_BASE_URL must be an HTTP(S) URL."
}

android {
    namespace = "com.smartsolar.microgrid"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smartsolar.microgrid"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Override for a physical device or isolated API verification without editing client code.
        buildConfigField("String", "API_BASE_URL", "\"" +
            apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")

        manifestPlaceholders["MAPS_API_KEY"] =
            System.getenv("MAPS_API_KEY")
                ?: environmentProperties.getProperty("MAPS_API_KEY")
                ?: localProperties.getProperty("MAPS_API_KEY")
                ?: ""

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
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // QR Code Generation
    implementation("com.google.zxing:core:3.5.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
