plugins { id("com.android.application") }

// Keystore bersama untuk debug + release (diisi oleh GitHub Actions / gradle.properties lokal)
val ksPath: String? = System.getenv("RESTEAM_KEYSTORE") ?: (findProperty("RESTEAM_KEYSTORE") as String?)
fun secret(name: String): String? = System.getenv(name) ?: (findProperty(name) as String?)

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
        if (ksPath != null) {
            create("shared") {
                storeFile = file(ksPath)
                storePassword = secret("RESTEAM_KEYSTORE_PASSWORD")
                keyAlias = secret("RESTEAM_KEY_ALIAS")
                keyPassword = secret("RESTEAM_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("debug") { if (ksPath != null) signingConfig = signingConfigs.getByName("shared") }
        getByName("release") {
            isMinifyEnabled = false
            if (ksPath != null) signingConfig = signingConfigs.getByName("shared")
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
