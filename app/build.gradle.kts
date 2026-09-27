plugins {
    id("com.android.application")
}

android {
    namespace = "com.dellybizz.notificationvault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dellybizz.notificationvault"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-phase1"

        buildConfigField("String", "API_BASE_URL", "\"https://notification-vault-6lthey.v2.appdeploy.ai\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}
