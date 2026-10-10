import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText.orNull?.let { load(it.reader()) }
}
val chatAesKey: String = providers.gradleProperty("chatAesKey").orNull
    ?: localProperties.getProperty("chat.aesKey")
    ?: ""

android {
    namespace = "io.github.mrtabaofficial.wardline"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.mrtabaofficial.wardline"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "CHAT_AES_KEY", "\"$chatAesKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

val checkChatAesKey = tasks.register("checkChatAesKey") {
    val key = chatAesKey
    doLast {
        val valid = runCatching { Base64.getDecoder().decode(key).size == 32 }.getOrDefault(false)
        check(valid) {
            "Set chat.aesKey in local.properties to a Base64-encoded 32-byte key, " +
                "for example the output of: openssl rand -base64 32"
        }
    }
}

tasks.named("preBuild") { dependsOn(checkChatAesKey) }
