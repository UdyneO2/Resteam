plugins { id("com.android.application") }

// Versi bisa di-override dari CI: -PversionName=1.2-beta -PversionCode=12
val vName = (project.findProperty("versionName") as String?) ?: "1.2-beta"
val vCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 3

android {
    namespace = "com.udyneos.resteam"
    compileSdk = 34
    ndkVersion = "26.1.10909125"
    defaultConfig {
        applicationId = "com.udyneos.resteam"
        minSdk = 29
        targetSdk = 34
        versionCode = (findProperty("appVersionCode") as String?)?.toInt() ?: 10600
        versionName = (findProperty("appVersionName") as String?) ?: "1.6"
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild { cmake { cppFlags += "-std=c++17" } }
    }
    signingConfigs {
        create("release") {
            val path = System.getenv("KEYSTORE_PATH")
            if (path != null && file(path).exists()) {
                storeFile = file(path)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Lokal tanpa keystore -> fallback ke debug key; di CI selalu pakai keystore rilis
            signingConfig = if (System.getenv("KEYSTORE_PATH") != null)
                signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    lint { abortOnError = false; checkReleaseBuilds = false }
}
dependencies {
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
