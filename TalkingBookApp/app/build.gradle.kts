import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("io.sentry.android.gradle")
}

android {
    // Load versionCode from version.properties file.
    val versionProps = Properties()
    val versionPropsFile = rootProject.file("version.properties")
    if (versionPropsFile.exists()) {
        versionProps.load(FileInputStream(versionPropsFile))
    }
    var code = (versionProps["versionCode"]).toString().toInt()

    // If this is a release build (ie, will be signed for the play store), increment versionCode, and save.
    gradle.startParameter.taskNames.forEach {
        if (it.contains("assembleRelease") or it.contains("bundleRelease")) {
            code += 1
            versionProps["versionCode"] = code.toString()
            versionProps.store(versionPropsFile.outputStream(), null)
        }
    }

    namespace = "org.literacybridge.tbloaderandroid"
    compileSdk = 34

    hilt {
        enableAggregatingTask = true
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "org.literacybridge.tbloaderandroid"
        minSdk = 24
        targetSdk = 34
        versionCode = code
        versionName = "2.0.0_${code}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Env variables
        buildConfigField("Boolean", "DEBUG_MODE", "true")
        buildConfigField(
            "String",
            "API_URL",
            "\"https://nhr12r5plj.execute-api.us-west-2.amazonaws.com/dev/\""
        )

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        create("release") {
            // Load key store credentials from keystore.properties file
            val keyProps = Properties()
            val keyPropsFile = rootProject.file("keystore.properties")
            if (keyPropsFile.exists()) {
                keyProps.load(FileInputStream(keyPropsFile))
            } else {
                throw GradleException("'keystore.properties' file no found! Create a new file from 'key.properties.example'")
            }

            // You need to specify either an absolute path or include the
            // keystore file in the same directory as the build.gradle file.
            storeFile = file(keyProps["STORE_FILE"].toString())
            storePassword = keyProps["STORE_PASSWORD"].toString()
            keyAlias = keyProps["KEY_ALIAS"].toString()
            keyPassword = keyProps["KEY_PASSWORD"].toString()
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            ndk.debugSymbolLevel = "FULL"
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("Boolean", "DEBUG_MODE", "false")
            buildConfigField(
                "String",
                "API_URL",
                "\"https://nhr12r5plj.execute-api.us-west-2.amazonaws.com/production/\""
            )
        }

        debug {
            isMinifyEnabled = false
            buildConfigField("Boolean", "DEBUG_MODE", "true")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val navVersion = "2.7.7"
    val roomVersion = "2.6.1"

    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // Others
    implementation(project(":core"))
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.sentry:sentry-android:7.8.0")
    implementation("io.sentry:sentry-compose-android:7.8.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    implementation("com.google.android.play:app-update:2.1.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    ksp("com.google.dagger:hilt-compiler:2.51")

    // OkHttp/Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.lifecycle:lifecycle-runtime-compose")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.navigation:navigation-compose:$navVersion")

    // Amplify
    implementation("com.amplifyframework:aws-auth-cognito:2.14.11")
    implementation("com.amplifyframework:core:2.14.11")
    implementation("com.amplifyframework.ui:authenticator:1.1.0")
    implementation("com.amplifyframework:aws-storage-s3:2.14.11")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Support for Java 8 features
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}