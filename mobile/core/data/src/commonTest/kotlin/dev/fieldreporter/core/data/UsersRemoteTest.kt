package dev.fieldreporter.core.data

import dev.fieldreporter.core.data.remote.KtorIncidentRemote
import dev.fieldreporter.core.data.remote.RemoteException
import dev.fieldreporter.core.data.remote.UserDto
import dev.fieldreporter.core.data.remote.toDomain
import dev.fieldreporter.core.data.local.decodeKnownUsers
import dev.fieldreporter.core.domain.Role
import dev.fieldreporter.core.domain.User
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** `GET /users` at the remote boundary, and the per-user mapping used by the remote and the stored list. */
class UsersRemoteTest {
    private val mya = User("u-op-1", "Mya Sandar", Role.OPERATOR)
    private val htet = User("u-op-2", "Htet Naing", Role.OPERATOR)

    private fun remoteAnswering(body: String, type: ContentType = ContentType.Application.Json) = KtorIncidentRemote(
        client = HttpClient(MockEngine { respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, type.toString())) }) {
            fieldReporterConfig()
        },
        baseUrl = "http://api.test",
        userId = { null },
    )

    @Test
    fun aUsersBodyThatDoesNotDecodeBecomesANetworkFailure() = runTest {
        val undecodable = listOf(
            """[{"id":"u-op-1","role":"OPERATOR"}]""", // a user missing `name`
            """[{"id":"u-op-1","name":"Mya Sandar","role":null}]""",
            """{"id":"u-op-1"}""", // not an array
            """not json""",
        )
        for (body in undecodable) {
            assertFailsWith<RemoteException.Network>(body) { remoteAnswering(body).users() }
        }
        assertFailsWith<RemoteException.Network>("a non-JSON content type") {
            remoteAnswering("<html></html>", ContentType.Text.Html).users()
        }
    }

    @Test
    fun aUserWithARoleThisBuildDoesNotKnowIsSkippedAndTheRestAreKept() = runTest {
        val body = """[
            {"id":"u-op-1","name":"Mya Sandar","role":"OPERATOR"},
            {"id":"u-aud-1","name":"Someone","role":"AUDITOR"},
            {"id":"u-op-2","name":"Htet Naing","role":"OPERATOR"}
        ]"""

        assertEquals(listOf(mya, htet), remoteAnswering(body).users())
        assertEquals(
            listOf(mya, htet),
            listOf(UserDto("u-op-1", "Mya Sandar", "OPERATOR"), UserDto("u-aud-1", "Someone", "AUDITOR"), UserDto("u-op-2", "Htet Naing", "OPERATOR")).toDomain(),
        )
    }

    @Test
    fun theStoredListSkipsAnUnknownRoleAndKeepsEveryOperator() {
        val stored = """[
            {"id":"u-op-1","name":"Mya Sandar","role":"OPERATOR"},
            {"id":"u-aud-1","name":"Someone","role":"AUDITOR"},
            {"id":"u-op-2","name":"Htet Naing","role":"OPERATOR"}
        ]"""

        assertEquals(listOf(mya, htet), decodeKnownUsers(stored))
        assertEquals(emptyList(), decodeKnownUsers("not json"), "an undecodable value reads as no list")
        assertEquals(emptyList(), decodeKnownUsers(null))
    }
}
