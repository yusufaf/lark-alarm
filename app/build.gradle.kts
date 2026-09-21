plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.yusufaf.lark"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.yusufaf.lark"
        minSdk = 30
        targetSdk = 36
        versionName = "1.0.0" // x-release-please-version

        // release-please bumps versionName; versionCode follows it so a
        // semver bump is always an upgrade on the watch. Only the numeric core
        // counts, so a prerelease suffix like -rc.1 doesn't break the build.
        val (major, minor, patch) = Regex("""(\d+)\.(\d+)\.(\d+)""")
            .find(versionName!!)!!.destructured.toList().map(String::toInt)
        // 0.0.0 is the unreleased placeholder; Android rejects versionCode 0.
        versionCode = maxOf(major * 10_000 + minor * 100 + patch, 1)
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Debug-signed so the release build can be sideloaded for testing;
            // a real release keystore comes with the first published build.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.wear.compose.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
