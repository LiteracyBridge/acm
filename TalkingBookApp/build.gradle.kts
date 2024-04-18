// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
    id("io.sentry.android.gradle") version "4.4.1" apply false
    id("io.sentry.kotlin.compiler.gradle") version "4.4.1"
}