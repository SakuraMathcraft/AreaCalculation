plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 35
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

    buildFeatures {
        viewBinding = true // 如果你使用 ViewBinding，可保留
    }
}

dependencies {
    // ✅ 高德地图 SDK
    implementation("com.amap.api:3dmap:9.8.3")

    // ✅ Google 官方库
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.activity)

    // ✅ 单元测试
    testImplementation(libs.junit)

    // ✅ Android Instrumentation 测试
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
