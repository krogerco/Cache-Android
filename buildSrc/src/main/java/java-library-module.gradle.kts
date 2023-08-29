plugins {
    kotlin("jvm")
    `java-library`
    id("org.jetbrains.dokka")
}

kotlin {
    jvmToolchain(11)
    explicitApi()
}

tasks {
    test {
        useJUnitPlatform()
    }
}
