package com.example.plugins

import com.example.model.Pokemon
import com.example.model.PokemonType
import com.example.model.ResponsePokemon
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

fun Application.configureRouting() {

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    suspend fun getPokemonBy(name: String?, scope: CoroutineScope): Pokemon {
        val deferredResultInfo: Deferred<JsonObject> = scope.async {
            httpClient.get("https://pokeapi.co/api/v2/pokemon/$name").body()
        }

        return deferredResultInfo.await().run {
            val id = (this["id"] as JsonPrimitive).intOrNull
            val weight = (this["weight"] as? JsonPrimitive)?.intOrNull
            val height = (this["height"] as? JsonPrimitive)?.intOrNull
            val types = this["types"]?.jsonArray ?: error("types is not json array")
            val pokemonTypes = types.map {
                val type = it.jsonObject["type"] ?: error("error get type")
                val name = (type.jsonObject["name"] as JsonPrimitive).content
                name
            }

            Pokemon(name = name, id = id, height = height, weight = weight, types = pokemonTypes)
        }
    }

    suspend fun <T> retry(
        numberOfRetries: Int,
        delayBetweenRetries: Long = 100,
        block: suspend () -> T
    ) : T {
        repeat(numberOfRetries) {
            try {
                return block()
            } catch (e: Exception) {
                println("Error: $e")
            }
            delay(delayBetweenRetries)
        }

        return block()
    }


    routing {

        /*static(remotePath = "assets") {
            resources(resourcePackage = "static")
        }*/

        get("/") {
            call.respondText("Hello World!")
        }

        get("/users/{userName}") {
            val userName = call.parameters["userName"]
            val header = call.request.headers["Connection"]

            if (userName == "LuizGadao") {
                call.response.header(name = "custom-header", "admin")
                call.respond(message = "hello admin", status = HttpStatusCode.OK)
            }

            call.respondText("Greetings, $userName with header: $header")
        }

        get("/user") {
            val name = call.request.queryParameters["name"]
            val age = call.request.queryParameters["age"]

            call.respondText("Hello my name is $name, and I have $age")
        }

        get("/person") {
            try {
                val person = Person("Luiz", 99)
                call.respond(message = person, status = HttpStatusCode.OK)
            } catch (e: Exception) {
                call.respond(
                    message = "${e.message}",
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        get("/pokemon") {
            //val res = httpClient.get("https://pokeapi.co/api/v2/pokemon").bodyAsText()
            val res: ResponsePokemon =
                httpClient.get("https://pokeapi.co/api/v2/pokemon").body() //https://pokeapi.co/api/v2/pokemon?offset=0&limit=100
            //res["key"].jsonArray ?: error("error")
            //res.jsonObject["key"] ?: error("error")

            // get sync pokemon
            /*val myPokemons = res.results.map { pokemon ->
                val resultInfo: JsonObject = httpClient.get("https://pokeapi.co/api/v2/pokemon/${pokemon.name}").body()
                val types = resultInfo["types"]?.jsonArray ?: error("types is not json array")
                val pokemonTypes = types.map {
                    val type = it.jsonObject["type"] ?: error("error get type")
                    PokemonType(
                        name = type.jsonObject["name"].toString() ?: error("errot get name type")
                    )
                }
                pokemon.copy(types = pokemonTypes)
            }*/

            //get async
            val deferredPokemons = res.results.map { pokemon ->
                val deferredResultInfo: Deferred<JsonObject> = async {
                    httpClient.get("https://pokeapi.co/api/v2/pokemon/${pokemon.name}").body()
                }
                deferredResultInfo
            }

            val pokemons = deferredPokemons
                .awaitAll()
                .mapIndexed { index, resultInfo ->
                    val id = (resultInfo["id"] as JsonPrimitive).intOrNull
                    val weight = (resultInfo["weight"] as? JsonPrimitive)?.intOrNull
                    val height = (resultInfo["height"] as? JsonPrimitive)?.intOrNull
                    val types = resultInfo["types"]?.jsonArray ?: error("types is not json array")
                    val pokemonTypes = types.map {
                        val type = it.jsonObject["type"] ?: error("error get type")
                        (type.jsonObject["name"] as JsonPrimitive).content
                    }

                    res.results[index].copy(
                        id = id,
                        weight = weight,
                        height = height,
                        types = pokemonTypes
                    )
                }

            call.respond(
                message = pokemons
            )
        }

        get("/pokemons-async") {
            //val res = httpClient.get("https://pokeapi.co/api/v2/pokemon").bodyAsText()
            val res: ResponsePokemon =
                httpClient.get("https://pokeapi.co/api/v2/pokemon").body() //https://pokeapi.co/api/v2/pokemon?offset=0&limit=100

            //get async
            val deferredPokemons = res.results.map { pokemon ->
                val deferredResultInfo: Deferred<JsonObject> = async {
                    httpClient.get("https://pokeapi.co/api/v2/pokemon/${pokemon.name}").body()
                }
                deferredResultInfo
            }

            val pokemons = deferredPokemons
                .awaitAll()
                .mapIndexed { index, resultInfo ->
                    val id = (resultInfo["id"] as JsonPrimitive).intOrNull
                    val weight = (resultInfo["weight"] as? JsonPrimitive)?.intOrNull
                    val height = (resultInfo["height"] as? JsonPrimitive)?.intOrNull
                    val types = resultInfo["types"]?.jsonArray ?: error("types is not json array")
                    val pokemonTypes = types.map {
                        val type = it.jsonObject["type"] ?: error("error get type")
                        val name = (type.jsonObject["name"] as JsonPrimitive).content
                        name
                    }

                    res.results[index].copy(
                        id = id,
                        weight = weight,
                        height = height,
                        types = pokemonTypes
                    )
                }

            call.respond(
                message = pokemons
            )
        }

        get("/pokemons-async-v2") {
            //val res = httpClient.get("https://pokeapi.co/api/v2/pokemon").bodyAsText()
            val res: ResponsePokemon =
                httpClient.get("https://pokeapi.co/api/v2/pokemon?offset=0&limit=100").body()

            //get async with retry
            val pokemons = res.results.map { pokemon ->
                retry(
                    numberOfRetries = 3,
                    delayBetweenRetries = 100L,
                ) {
                    getPokemonBy(name = pokemon.name, scope = this)
                }
            }

            call.respond(
                message = pokemons
            )
        }

    }
}
/*
suspend fun getPokemonBy(name: String?, scope: CoroutineScope): Pokemon {
    val deferredResultInfo: Deferred<JsonObject> = scope.async {
        httpClient.get("https://pokeapi.co/api/v2/pokemon/$name").body()
    }

    return deferredResultInfo.await().run {
        val id = (this["id"] as JsonPrimitive).intOrNull
        val weight = (this["weight"] as? JsonPrimitive)?.intOrNull
        val height = (this["height"] as? JsonPrimitive)?.intOrNull
        val types = this["types"]?.jsonArray ?: error("types is not json array")
        val pokemonTypes = types.map {
            val type = it.jsonObject["type"] ?: error("error get type")
            PokemonType(
                name = (type.jsonObject["name"] as JsonPrimitive).content ?: error("errot get name type")
            )
        }

        Pokemon(name = name, id = id, height = height, weight = weight, types = pokemonTypes)
    }
}
 */

@Serializable
data class Person(val name: String, val age: Int)
