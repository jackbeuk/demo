package com.example.demo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("competitions")
data class Competition(
    @Id
    val name: String,
    val teams: MutableList<Team> = mutableListOf(),
    val speelronden: MutableList<Speelronde> = mutableListOf()
)

data class CreateCompetitionRequest(val name: String)
data class AddTeamToCompetitionRequest(val teamId: Long)
