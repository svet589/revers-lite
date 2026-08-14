plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.revers.messenger"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.revers.messenger"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "SERVER_URL", "\"${project.findProperty("SERVER_URL") ?: "http://10.0.2.2:3000"}\"")
        buildConfigField("String", "WS_URL", "\"${project.findProperty("WS_URL") ?: "ws://10.0.2.2:3000"}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Room
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("io.socket:socket.io-client:2.0.1")

    // DI
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Crypto
    implementation("com.google.crypto.tink:tink-android:1.9.0")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

// ====================================================
// 🎨 АВТОМАТИЧЕСКАЯ ГЕНЕРАЦИЯ ИКОНОК
// ====================================================

tasks.register("generateIcons") {
    description = "Генерирует иконки приложения"
    group = "build"

    doLast {
        val resDir = projectDir.resolve("src/main/res")

        // Создаём папки
        val drawableDir = resDir.resolve("drawable").apply { mkdirs() }
        val anydpiDir = resDir.resolve("mipmap-anydpi-v26").apply { mkdirs() }
        val valuesDir = resDir.resolve("values").apply { mkdirs() }

        // 1. Векторная иконка (передний план)
        drawableDir.resolve("ic_launcher_foreground.xml").writeText(
            """<?xml version="1.0" encoding="utf-8"?>
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="108dp"
                android:height="108dp"
                android:viewportWidth="108"
                android:viewportHeight="108">
                <path
                    android:fillColor="#E63946"
                    android:pathData="M30,22 L30,86 L44,86 L44,56 L64,56 L64,86 L78,86 L78,56 C78,49.7 72.9,44 66.6,44 L78,44 L78,34 C78,27.7 72.9,22 66.6,22 L30,22 Z M44,34 L66,34 C69.3,34 72,36.7 72,40 L72,44 L44,44 L44,34 Z"/>
                <path
                    android:fillColor="#FFFFFF"
                    android:pathData="M90,30 L92,25 L94,30 L99,32 L94,34 L92,39 L90,34 L85,32 L90,30 Z"/>
            </vector>"""
        )

        // 2. Фон иконки
        drawableDir.resolve("ic_launcher_background.xml").writeText(
            """<?xml version="1.0" encoding="utf-8"?>
            <shape xmlns:android="http://schemas.android.com/apk/res/android"
                android:shape="rectangle">
                <solid android:color="#0F0F12" />
                <corners android:radius="16dp" />
            </shape>"""
        )

        // 3. Adaptive icon
        val adaptiveIcon = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>"""

        anydpiDir.resolve("ic_launcher.xml").writeText(adaptiveIcon)
        anydpiDir.resolve("ic_launcher_round.xml").writeText(adaptiveIcon)

        // 4. Цвета
        valuesDir.resolve("colors.xml").writeText(
            """<?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="ic_launcher_background">#0F0F12</color>
                <color name="ic_launcher_foreground">#E63946</color>
                <color name="purple_200">#FFBB86FC</color>
                <color name="purple_500">#FF6200EE</color>
                <color name="purple_700">#FF3700B3</color>
                <color name="teal_200">#FF03DAC5</color>
                <color name="teal_700">#FF018786</color>
                <color name="black">#FF000000</color>
                <color name="white">#FFFFFFFF</color>
            </resources>"""
        )

        // 5. Создаём PNG иконки (пустые файлы, будут заполнены позже)
        val sizes = listOf(
            "mipmap-mdpi" to 48,
            "mipmap-hdpi" to 72,
            "mipmap-xhdpi" to 96,
            "mipmap-xxhdpi" to 144,
            "mipmap-xxxhdpi" to 192
        )

        sizes.forEach { (folder, size) ->
            val targetDir = resDir.resolve(folder).apply { mkdirs() }
            val pngFile = targetDir.resolve("ic_launcher.png")
            val pngRoundFile = targetDir.resolve("ic_launcher_round.png")

            if (!pngFile.exists()) {
                // Создаём минимальный PNG (1x1 пиксель)
                val pixel = byteArrayOf(
                    0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                    0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4,
                    0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
                    0x54, 0x78, 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
                    0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4, 0x00,
                    0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE,
                    0x42, 0x60, 0x82
                )
                pngFile.writeBytes(pixel)
                pngRoundFile.writeBytes(pixel)
            }
        }

        println("✅ Иконки сгенерированы в ${resDir.absolutePath}")
    }
}

// Запускаем генерацию перед сборкой
tasks.named("preBuild") {
    dependsOn("generateIcons")
}
