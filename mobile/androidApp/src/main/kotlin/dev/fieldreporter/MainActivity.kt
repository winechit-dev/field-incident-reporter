package dev.fieldreporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.fieldreporter.core.domain.IncidentRepository
import dev.fieldreporter.core.domain.UserRepository
import dev.fieldreporter.feature.incidents.App
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val incidents = GlobalContext.get().get<IncidentRepository>()
        val users = GlobalContext.get().get<UserRepository>()
        setContent { App(incidents, users) }
    }
}
