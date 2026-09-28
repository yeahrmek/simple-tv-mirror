package io.github.jqssun.airplay.diag

import fi.iki.elonen.NanoHTTPD

/** Serves the event journal on the local network, since YaOS gives no adb access. */
class DiagServer(port: Int, private val logs: () -> String) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response = when (session.uri) {
        "/logs" -> newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", logs())
        else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "not found")
    }
}
