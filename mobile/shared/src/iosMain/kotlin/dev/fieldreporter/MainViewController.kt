package dev.fieldreporter

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import dev.fieldreporter.core.domain.IncidentRepository
import dev.fieldreporter.core.domain.UserRepository
import dev.fieldreporter.feature.incidents.App
import dev.fieldreporter.platform.IosPlatformCapabilities

fun MainViewController() = ComposeUIViewController {
    val capabilities = remember { IosPlatformCapabilities() }
    App(IosApp.koin.get<IncidentRepository>(), IosApp.koin.get<UserRepository>(), capabilities)
}
