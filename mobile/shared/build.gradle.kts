plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Umbrella module: it only exists to build the single iOS framework ("Shared") that Xcode links.
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(project(":feature:incidents"))
        }
    }
    android {
        namespace = "dev.fieldreporter.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":feature:incidents"))
            implementation(project(":core:data"))
            implementation(libs.koin.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
        }
    }
}
