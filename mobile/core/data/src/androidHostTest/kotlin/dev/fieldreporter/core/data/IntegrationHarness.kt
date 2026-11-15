package dev.fieldreporter.core.data

import dev.fieldreporter.core.data.remote.KtorIncidentRemote
import dev.fieldreporter.core.data.repository.IncidentRepositoryImpl
import dev.fieldreporter.core.data.sync.SyncEngine
import dev.fieldreporter.core.domain.Role
import dev.fieldreporter.core.domain.SyncScheduler
import dev.fieldreporter.core.domain.User
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.assertTrue

/**
 * The app's real sync engine and Ktor client, pointed at a real API, plus an Operator who acts over
 * HTTP. "Offline" in these tests means not calling `sync()`: a change is saved locally and queued,
 * exactly as it is on a phone with no network.
 *
 * The integration tests run only when the environment names a real API; `scripts/integration` sets
 * `FIR_API_URL` and the shared Operator patches, and starts the API.
 */
internal class IntegrationHarness(val apiUrl: String) {
    val technician = User("u-tech-1", "Aung Kyaw", Role.TECHNICIAN)
    val store = FakeLocalStore()
    val remote = KtorIncidentRemote(createHttpClient(), apiUrl, userId = { technician.id })
    val repo = IncidentRepositoryImpl(
        store = store,
        engine = SyncEngine(store, remote) { },
        scheduler = SyncScheduler { /* offline: nothing runs until the test calls sync() */ },
        deleteFile = { },
    )
    private val operator = createHttpClient()

    init {
        runBlocking { store.setCurrentUser(technician) }
    }

    /** What the dashboard does when an Operator acts: a PATCH as the Operator, with a patch the dashboard really sends. */
    fun operatorPatches(incidentId: String, patchJson: String) = runBlocking {
        val response = operator.patch("$apiUrl/incidents/$incidentId") {
            header("X-User-Id", "u-op-1")
            contentType(ContentType.Application.Json)
            setBody(patchJson)
        }
        assertTrue(response.status.isSuccess(), "the Operator's change should succeed: ${response.bodyAsText()}")
    }

    /** How many incidents the Operator's list shows for a search. */
    fun operatorListCount(search: String): Int = runBlocking {
        val response: HttpResponse = operator.get("$apiUrl/incidents?q=${search.replace(" ", "%20")}") {
            header("X-User-Id", "u-op-1")
        }
        Json.parseToJsonElement(response.bodyAsText()).jsonArray.size
    }

    /** The bytes a client downloads from a photo URL. */
    fun download(url: String): ByteArray = runBlocking { operator.get(url).readRawBytes() }

    /** Hands what the app created to the dashboard phase, which checks the dashboard sees the same. */
    fun writeHandoff(name: String, values: Map<String, Any?>) {
        val dir = System.getenv("FIR_STATE_DIR") ?: return
        File(dir).mkdirs()
        val json = buildJsonObject {
            values.forEach { (key, value) ->
                put(key, when (value) {
                    null -> kotlinx.serialization.json.JsonNull
                    is Number -> JsonPrimitive(value)
                    else -> JsonPrimitive(value.toString())
                })
            }
        }
        File(dir, "$name.json").writeText(json.toString())
    }

    companion object {
        /** Standing in for a photo: the API stores bytes, so these only need to be recognisable. */
        val PHOTO_BYTES: ByteArray = Base64.getDecoder().decode(
            "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=",
        )

        fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        fun tempPhoto(): String = File.createTempFile("incident-", ".jpg").apply {
            writeBytes(PHOTO_BYTES)
            deleteOnExit()
        }.absolutePath
    }
}
