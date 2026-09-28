package io.github.jqssun.airplay.diag

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Persistent event journal: events.0.log is current, events.N.log are older.
 * Survives process restarts so the cause of a disconnect can be read afterwards.
 */
class EventLog(
    private val dir: File,
    private val maxBytes: Long = 2_000_000,
    private val maxFiles: Int = 5,
    zone: TimeZone = TimeZone.getDefault(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).apply { timeZone = zone }

    init { dir.mkdirs() }

    private fun file(i: Int) = File(dir, "events.$i.log")

    @Synchronized fun write(msg: String) {
        val line = "${format.format(Date(now()))} $msg\n".toByteArray()
        if (file(0).length() + line.size > maxBytes) rotate()
        file(0).appendBytes(line)
    }

    @Synchronized fun read(): String =
        (maxFiles - 1 downTo 0).map(::file).filter { it.exists() }.joinToString("") { it.readText() }

    private fun rotate() {
        file(maxFiles - 1).delete()
        for (i in maxFiles - 2 downTo 0) file(i).takeIf { it.exists() }?.renameTo(file(i + 1))
    }
}
