plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.one_tech"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.one_tech"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Firebase BOM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Firebase зависимости
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // AndroidX зависимости из version catalog
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // RecyclerView для списков
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Lifecycle для ViewModel/LiveData (если понадобится)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Glide для загрузки изображений
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Navigation Component (опционально, для улучшенной навигации)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    // Тестовые зависимости
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Retrofit для API (если будете подключать платежи)
    // implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}