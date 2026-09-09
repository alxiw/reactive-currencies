plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.ksp)
}

android {
    namespace = "io.github.alxiw.reactivecurrencies"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.alxiw.reactivecurrencies"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures.viewBinding = true
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)

    implementation(libs.rxjava)
    implementation(libs.rxandroid)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.xml)
    implementation(libs.retrofit.adapter.rxjava3)
    implementation(libs.logging.interceptor)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.rxjava3)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
