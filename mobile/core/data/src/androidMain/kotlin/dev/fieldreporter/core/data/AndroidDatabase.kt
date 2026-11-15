package dev.fieldreporter.core.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.fieldreporter.core.data.local.FieldReporterDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidDatabaseModule(context: Context): Module = module {
    single<FieldReporterDatabase> {
        Room.databaseBuilder<FieldReporterDatabase>(
            context = context.applicationContext,
            name = context.applicationContext.getDatabasePath("field_reporter.db").absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}
