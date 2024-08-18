import com.android.build.gradle.AppExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

apply<EchoExtensionPlugin>()
configure<EchoExtension> {
    versionCode = 1
    versionName = "1.0.0"
    extensionClass = "TestExtension"
    id = "test"
    name = "Test Music"
    description = "A Test Extension"
    author = "Test Author"
}

dependencies {
    implementation(project(":ext"))
    val libVersion: String by project
    compileOnly("com.github.brahmkshatriya:echo:$libVersion")
}

android {
    namespace = "dev.brahmkshatriya.echo.extension"
    compileSdk = 34
    defaultConfig {
        applicationId = "dev.brahmkshatriya.echo.extension.test"
    }

    buildTypes {
        all {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

open class EchoExtension {
    var extensionClass: String? = null
    var id: String? = null
    var name: String? = null
    var description: String? = null
    var author: String? = null
    var iconUrl: String? = null
    var versionCode: Int? = null
    var versionName: String? = null
}

abstract class EchoExtensionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val echoExtension = project.extensions.create("echoExtension", EchoExtension::class.java)
        project.afterEvaluate {
            project.extensions.configure<AppExtension>("android") {
                defaultConfig.apply {
                    minSdk = 24
                    targetSdk = 34
                    with(echoExtension) {
                        resValue("string", "id", id!!)
                        resValue("string", "name", name!!)
                        resValue("string", "app_name", "Echo : $name Extension")
                        val extensionClass = extensionClass!!
                        resValue("string", "class_path", "$namespace.$extensionClass")
                        resValue("string", "version", versionName!!)
                        resValue("string", "description", description!!)
                        resValue("string", "author", author!!)
                        iconUrl?.let { resValue("string", "icon_url", it) }
                    }
                }
            }
        }
    }
}