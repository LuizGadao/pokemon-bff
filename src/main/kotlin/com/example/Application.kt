package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.example.plugins.*
import io.ktor.server.plugins.callloging.*

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module,
        watchPaths = listOf("classes", "resources"),
    ).start(wait = true)
}

fun Application.module() {
    install(CallLogging)
    configureSerialization()
    configureRouting()
}
