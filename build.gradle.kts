/**
 * MIT License
 *
 * Copyright (c) 2023 The Kroger Co. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
plugins {
    alias(libs.plugins.conventions.androidApplication) apply false
    alias(libs.plugins.conventions.publishedAndroidLibrary) apply false
    alias(libs.plugins.conventions.publishedKotlinLibrary) apply false
    alias(libs.plugins.conventions.root)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.junit5) apply false
    alias(libs.plugins.dependencyAnalysis) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.gradleVersions) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kapt) apply false
    alias(libs.plugins.kotlinter) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.mavenPublish) apply false
}
