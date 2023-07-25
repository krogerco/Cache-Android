plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.android.tools.build:gradle:7.4.2")
    implementation("de.mannodermaus.gradle.plugins:android-junit5:1.8.2.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.7.20")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.7.20")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.25.2")
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.43.2")
}
