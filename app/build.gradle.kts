plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.securevpn.facedetactionattandencesystem"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.securevpn.facedetactionattandencesystem"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")


    implementation ("com.google.firebase:firebase-ml-vision:24.0.3")
    // If you want to detect face contours (landmark detection and classification
    // don't require this additional model):
    implementation ("com.google.firebase:firebase-ml-vision-face-model:20.0.1")
// Face features
    implementation ("com.google.android.gms:play-services-mlkit-face-detection:17.0.1")

    // camera x
    implementation ("androidx.camera:camera-camera2:1.3.0-beta01")
    implementation ("androidx.camera:camera-lifecycle:1.3.0-beta01")
    implementation ("androidx.camera:camera-view:1.3.0-beta01")
    implementation ("androidx.camera:camera-extensions:1.3.0-beta01")
    implementation ("androidx.camera:camera-core:1.3.0-beta01")
}