package dev.fieldreporter.core.data.remote

import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.IncidentEdit
import dev.fieldreporter.core.domain.Photo
import dev.fieldreporter.core.domain.User
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class KtorIncidentRemote(
    private val client: HttpClient,
    private val baseUrl: String,
    /** The Current User's id, sent as `X-User-Id`. */
    private val userId: suspend () -> String?,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : IncidentRemote {

    override suspend fun users(): List<User> =
        decoding { send { get { endpoint("/users") } }.body<List<UserDto>>() }.toDomain()

    override suspend fun listMine(userId: String): List<Incident> =
        send { get { endpoint("/incidents?reportedBy=$userId&_sort=createdAt&_order=desc") } }
            .body<List<IncidentDto>>().map { it.toDomain() }

    override suspend fun get(id: String): Incident =
        send { get { endpoint("/incidents/$id") } }.body<IncidentDto>().toDomain()

    override suspend fun create(incident: Incident): Incident = send {
        post {
            endpoint("/incidents")
            contentType(ContentType.Application.Json)
            setBody(incident.toCreateRequest())
        }
    }.body<IncidentDto>().toDomain()

    override suspend fun update(id: String, edit: IncidentEdit): Incident = send {
        patch {
            endpoint("/incidents/$id")
            contentType(ContentType.Application.Json)
            setBody(edit.toRequest())
        }
    }.body<IncidentDto>().toDomain()

    override suspend fun delete(id: String) {
        send { delete { endpoint("/incidents/$id") } }
    }

    override suspend fun uploadPhoto(incidentId: String, photoId: String, localPath: String): Photo {
        val bytes = try {
            SystemFileSystem.source(Path(localPath)).buffered().use { it.readByteArray() }
        } catch (e: IOException) {
            // The local file is gone; retrying cannot help.
            throw RemoteException.Rejected(0, "Photo file is missing")
        }
        return send {
            post {
                endpoint("/incidents/$incidentId/photos")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("id", photoId)
                            append("file", bytes, Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"$photoId.jpg\"")
                            })
                        },
                    ),
                )
            }
        }.body<PhotoDto>().toDomain()
    }

    override suspend fun deletePhoto(incidentId: String, photoId: String) {
        send { delete { endpoint("/incidents/$incidentId/photos/$photoId") } }
    }

    private fun HttpRequestBuilder.endpoint(path: String) {
        url(baseUrl.trimEnd('/') + path)
    }

    /** Runs a request with the user header and maps failures to [RemoteException]. */
    private suspend fun send(request: suspend Authed.() -> HttpResponse): HttpResponse {
        val response = try {
            Authed(client, userId()).request()
        } catch (e: IOException) {
            throw RemoteException.Network(e.message ?: "Network unavailable")
        } catch (e: SerializationException) {
            throw RemoteException.Network("Unreadable response")
        }
        if (response.status.isSuccess()) return response
        val message = runCatching { json.decodeFromString<ErrorEnvelope>(response.bodyAsText()).error.message }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: response.status.description
        throw when (val code = response.status.value) {
            401 -> RemoteException.Unauthorized(message)
            404 -> RemoteException.NotFound(message)
            409 -> RemoteException.Conflict(message)
            in 400..499 -> RemoteException.Rejected(code, message)
            else -> RemoteException.Network(message)
        }
    }

    /**
     * Reads a response body, mapping a body that does not decode (wrong shape, missing field, not
     * JSON) to [RemoteException.Network], as [send] does for failures during the request. Ktor wraps
     * decode failures in [ContentConvertException] and reports an unexpected content type with
     * [NoTransformationFoundException]; neither is a [RemoteException].
     */
    private suspend fun <T> decoding(read: suspend () -> T): T = try {
        read()
    } catch (e: ContentConvertException) {
        throw RemoteException.Network("Unreadable response")
    } catch (e: NoTransformationFoundException) {
        throw RemoteException.Network("Unreadable response")
    } catch (e: SerializationException) {
        throw RemoteException.Network("Unreadable response")
    }

    /** Small helper so each call adds `X-User-Id` without repeating it. */
    private class Authed(private val client: HttpClient, private val userId: String?) {
        suspend fun get(block: HttpRequestBuilder.() -> Unit) = client.get { headers(); block() }
        suspend fun post(block: HttpRequestBuilder.() -> Unit) = client.post { headers(); block() }
        suspend fun patch(block: HttpRequestBuilder.() -> Unit) = client.patch { headers(); block() }
        suspend fun delete(block: HttpRequestBuilder.() -> Unit) = client.delete { headers(); block() }
        private fun HttpRequestBuilder.headers() {
            if (userId != null) header("X-User-Id", userId)
        }
    }
}
