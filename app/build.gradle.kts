import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.hello"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.hello"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

val sqliteJniJars: Configuration = configurations.create("sqliteJniJars")

dependencies {
    sqliteJniJars(libs.androidx.sqlite.bundled.jvm)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.sqlite.bundled)
    implementation(libs.androidx.datastore.preferences)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.sqlite.bundled)
    testImplementation(libs.androidx.sqlite.bundled.jvm)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

val extractSqliteJni = tasks.register("extractSqliteJni") {
    val jniDir = layout.buildDirectory.dir("jniLibs")
    val jniFiles = sqliteJniJars.incoming.files
    inputs.files(jniFiles)
    outputs.dir(jniDir)
    doLast {
        val outDir = jniDir.get().asFile
        outDir.mkdirs()
        val jar = jniFiles.firstOrNull { it.name.contains("sqlite-bundled-jvm") }
        if (jar != null) {
            ZipFile(jar).use { zip ->
                val entry = zip.getEntry("natives/linux_x64/libsqliteJni.so")
                if (entry != null) {
                    zip.getInputStream(entry).use { input ->
                        File(outDir, "libsqliteJni.so").outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    dependsOn(extractSqliteJni)
    val jniDir = layout.buildDirectory.dir("jniLibs").get().asFile
    systemProperty("java.library.path", "${jniDir.absolutePath}:${System.getProperty("java.library.path")}")
}