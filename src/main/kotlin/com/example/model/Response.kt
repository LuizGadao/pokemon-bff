package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class ResponsePokemon(
    val count: Int,
    val results: List<Pokemon>
)

@Serializable
data class Pokemon(
    val name: String?,
    val id: Int? = null,
    val height: Int? = null,
    val weight: Int? = null,
    val types: List<String>? = mutableListOf()
)
@Serializable
data class PokemonType(
    val name: String?
)