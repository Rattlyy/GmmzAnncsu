package it.gmmz.anncsu.core

import klite.AssetsHandler
import klite.BadRequestException
import klite.Config
import klite.Server
import klite.isDev
import klite.json.JsonBody
import klite.metrics
import java.net.InetSocketAddress
import java.nio.file.Path

fun server(port: Int) = Server(InetSocketAddress("0.0.0.0", port)).apply {
    metrics()
    assets(
        "/", AssetsHandler(
            if (Config.isDev) Path.of("src", "main", "webapp")
            else Path.of("/app/webapp"),
    ))

    context("/api") {
        useOnly<JsonBody>()

        get("/search") {
            val q = query("q") ?: throw BadRequestException("Missing 'q' param")
            return@get Query.search(q)
        }
    }
}.start(20)