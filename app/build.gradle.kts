import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("org.jetbrains.dokka") version "2.2.0"
    id("org.jetbrains.dokka-javadoc") version "2.2.0"

}

android {
    namespace = "com.iesaguadulce.deambulario"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.iesaguadulce.deambulario"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    @Suppress("UnstableApiUsage")
    androidResources {
        localeFilters += listOf("es", "en")
    }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures{
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.glide)
    implementation(libs.firebase.storage)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.firebase.messaging)
    implementation(libs.material.tap.target.prompt)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
}


dokka {
    dokkaPublications.html {
        suppressInheritedMembers.set(true)
    }
}
