plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    id("com.chaquo.python") version "17.0.0" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
}

allprojects {
    afterEvaluate {
        extensions.findByName("android")?.let { ext ->
            val androidExt = ext as com.android.build.gradle.BaseExtension
            androidExt.ndkVersion = "29.0.14206865"
        }
    }
}
