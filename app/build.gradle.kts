plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ginkgoqwq.btgallery"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ginkgoqwq.btgallery"
        minSdk = 26
        targetSdk = 37
        // versionCode 必须是单调递增的整数，系统靠它判断“是否是新版本”；
        // versionName 只是展示用的字符串。发新版本时两者都要动。
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // 生成 BuildConfig，供设置页读取版本号（避免手写字符串与 Gradle 配置不一致）
        buildConfig = true
    }
}

dependencies {
    implementation("io.coil-kt:coil-compose:2.6.0")
    // Miuix：HyperOS / MIUI 设计语言（本项目唯一界面风格）
    implementation(libs.miuix)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}