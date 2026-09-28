package io.github.jqssun.airplay.diag

import java.net.HttpURLConnection
import java.net.URL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiagServerTest {
    private val server = DiagServer(port = 0) { "2026-09-21 14:13:20.123 Телевизор\n" }

    @Before fun start() = server.start()
    @After fun stop() = server.stop()

    private fun get(path: String) =
        URL("http://127.0.0.1:${server.listeningPort}$path").openConnection() as HttpURLConnection

    @Test fun `logs returns journal as utf-8 text`() {
        val conn = get("/logs")
        assertEquals(200, conn.responseCode)
        assertTrue(conn.contentType.startsWith("text/plain"))
        assertTrue(conn.contentType.contains("utf-8", ignoreCase = true))
        assertEquals("2026-09-21 14:13:20.123 Телевизор\n", conn.inputStream.readBytes().toString(Charsets.UTF_8))
    }

    @Test fun `unknown path is 404`() {
        assertEquals(404, get("/").responseCode)
    }
}
