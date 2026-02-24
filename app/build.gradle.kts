plugins {
    alias(libs.plugins.android.application)
    id("com.chaquo.python")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.elfilibustero.uabe"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.elfilibustero.uabe"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    ndkVersion = "29.0.14206865"
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

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    lint.abortOnError = false

    packagingOptions.jniLibs.apply {
        useLegacyPackaging = true
    }
}

val createMockGoogleServices: TaskProvider<Task?>? = tasks.register("createMockGoogleServices") {
    doLast {
        // If a real google-services.json already exists in this module, don't mock.
        val realFile = File(project.projectDir, "google-services.json")
        if (realFile.exists() && realFile.length() > 0L) return@doLast

        // Otherwise create debug mock file if it doesn't exist yet.
        val mockFile = file("src/debug/google-services.json")
        if (!mockFile.exists()) {
            mockFile.parentFile.mkdirs()

            val appId = android.defaultConfig.applicationId

            val mockContent = """
                {
                  "project_info": {
                    "project_number": "123456789000",
                    "firebase_url": "https://mock-project-default-rtdb.firebaseio.com",
                    "project_id": "mock-project",
                    "storage_bucket": "mock-project.appspot.com"
                  },
                  "client": [
                    {
                      "client_info": {
                        "mobilesdk_app_id": "1:123456789000:android:mockappid123456",
                        "android_client_info": {
                          "package_name": "$appId"
                        }
                      },
                      "oauth_client": [],
                      "api_key": [
                        { "current_key": "mock_AIzaSyBQJCUXVKUjD38-u5pWkqMtesIfFvxAcvs" }
                      ],
                      "services": {
                        "appinvite_service": { "other_platform_oauth_client": [] }
                      }
                    }
                  ],
                  "configuration_version": "1"
                }
            """.trimIndent()

            mockFile.writeText(mockContent)
        }
    }
}

tasks.named("preBuild") {
    dependsOn(createMockGoogleServices)
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation(libs.gson)

    implementation(libs.documentfile)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    implementation(project(":astc_encoder"))
    implementation(project(":etcpak"))
    implementation(project(":f3d"))
    implementation(project(":fmod"))
    implementation(project(":texture2ddecoder"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

chaquopy {
    defaultConfig {
        version = "3.10"
        val os = System.getProperty("os.name").lowercase()
        if (os.contains("windows")) {
            buildPython("C:/Windows/py.exe", "-3.10")
        } else {
            buildPython("python3.10")
        }
        pip {
            install("fsspec")
            install("brotli")
            install("lz4")
            install("attrs")
            install("pillow")

            install("pycryptodome")
        }
    }
    sourceSets {
        getByName("main") {
            srcDir("${rootDir}/third_party/UnityPy")
            srcDir("${rootDir}/astc_encoder/src/main/python")
            srcDir("${rootDir}/etcpak/src/main/python")
            srcDir("${rootDir}/fmod/src/main/python")
            srcDir("${rootDir}/texture2ddecoder/src/main/python")
        }
    }
}
