plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.mannodermaus.android.junit)
}

android {
    namespace = "io.github.alxiw.reactivecurrencies.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["runnerBuilder"] = "de.mannodermaus.junit5.AndroidJUnit5Builder"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            excludes += "META-INF/COPYRIGHT"
        }
    }
}

dependencies {
    implementation(project(":domain"))

    // Android
    implementation(libs.androidx.core.ktx)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.xml)
    implementation(libs.retrofit.adapter.rxjava3)
    implementation(libs.logging.interceptor)

    // Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.rxjava3)
    ksp(libs.androidx.room.compiler)

    // Prefs
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.rx3)

    // Tests
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.junit.jupiter)
    androidTestImplementation(libs.mannodermaus.android.test.core)
    androidTestRuntimeOnly(libs.mannodermaus.android.test.runner)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
