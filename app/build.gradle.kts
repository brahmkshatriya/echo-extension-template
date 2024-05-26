plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val extensionClass = "TestExtension"
val id = "test"
val name = "Test Music"
val version = "1.0.0"
val description = "A Test Extension"
val author = "Test Author"
val iconUrl: String? = null

android {
    namespace = "dev.brahmkshatriya.echo.extension"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.brahmkshatriya.echo.extension.test"
        minSdk = 24
        targetSdk = 34

        versionCode = 1
        versionName = version

        resValue("string", "app_name", "Echo : $name Extension")
        resValue("string", "class_path", "$namespace.$extensionClass")
        resValue("string", "name", name)
        resValue("string", "id", id)
        resValue("string", "version", version)
        resValue("string", "description", description)
        resValue("string", "author", author)
        iconUrl?.let { resValue("string", "icon_url", it) }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests {
            this.isReturnDefaultValues = true
        }
    }
}



dependencies {
    val libVersion = "38e1df03f6"
    compileOnly("com.github.brahmkshatriya:echo:$libVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1-Beta")
    testImplementation("com.github.brahmkshatriya:echo:$libVersion")
}