package com.example.demo

import org.springframework.data.annotation.Id

enum class Locatie { THUIS, UIT }

enum class Beschikbaarheid {
    VERHINDERD,
    BESCHIKBAAR,
    VOORKEUR
}

data class Speelronde(
    @Id
    val id: Long? = null,
    val locatie: Locatie,
    val datum: String,
    val tegenstander: Team?,
    val verhinderingen: MutableList<Verhindering> = mutableListOf(),
    val assignedPlayers: MutableList<Long> = mutableListOf()
)

data class Verhindering(
    val playerId: Long,
    val playerName: String,
    val beschikbaarheid: Beschikbaarheid
)

data class CreateSpeelrondeRequest(val locatie: Locatie, val datum: String, val tegenstander: Long)
data class SetVerhinderingRequest(val playerId: Long, val beschikbaarheid: Beschikbaarheid)
