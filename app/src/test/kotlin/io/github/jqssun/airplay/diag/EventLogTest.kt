package io.github.jqssun.airplay.diag

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EventLogTest {
    @get:Rule val tmp = TemporaryFolder()

    private val utc = TimeZone.getTimeZone("UTC")
    private var now = 1_790_000_000_123L  // 2026-09-21 14:13:20.123 UTC

    private fun log(maxBytes: Long = 1_000, maxFiles: Int = 3) =
        EventLog(tmp.root, maxBytes, maxFiles, utc) { now }

    @Test fun `line starts with local timestamp`() {
        val events = log()
        events.write("server started on port 7000")
        assertEquals("2026-09-21 14:13:20.123 server started on port 7000\n", events.read())
    }

    @Test fun `read returns lines in write order across rotations`() {
        val events = log(maxBytes = 100, maxFiles = 10)
        val msgs = (1..20).map { "event $it" }
        msgs.forEach(events::write)
        assertEquals(msgs, events.read().lines().filter { it.isNotEmpty() }.map { it.substringAfter(".123 ") })
    }

    @Test fun `keeps at most maxFiles and drops oldest`() {
        val events = log(maxBytes = 100, maxFiles = 3)
        (1..100).forEach { events.write("event $it") }
        assertTrue(tmp.root.listFiles()!!.size <= 3)
        val text = events.read()
        assertTrue(text.contains("event 100\n"))
        assertTrue(!text.contains("event 1\n"))
    }

    @Test fun `survives process restart`() {
        log().write("before restart")
        log().write("after restart")
        val lines = log().read().lines().filter { it.isNotEmpty() }
        assertEquals(listOf("before restart", "after restart"), lines.map { it.substringAfter(".123 ") })
    }

    @Test fun `keeps cyrillic`() {
        val events = log()
        events.write("Телевизор")
        assertTrue(events.read().endsWith("Телевизор\n"))
    }
}
