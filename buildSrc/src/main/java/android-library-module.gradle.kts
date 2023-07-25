plugins {
    id("com.android.library")
    kotlin("android")
    id("jacoco")
    id("de.mannodermaus.android-junit5")
    id("org.jetbrains.dokka")
}

android {
    compileSdk = SdkVersions.compileSdkVersion

    defaultConfig {
        minSdk = (SdkVersions.minSdkVersion)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packagingOptions {
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
    }
}

jacoco {
    toolVersion = "0.8.7"
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        kotlinOptions.freeCompilerArgs += "-Xexplicit-api=strict"
    }

    withType<JacocoReport> {
        reports {
            csv.required.set(false)
            html.required.set(false)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
