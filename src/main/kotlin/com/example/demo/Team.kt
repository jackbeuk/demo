package com.example.demo

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.annotation.Id

data class Team(
    @Id
    val id: Long? = null,
    val name: String,
    @JsonProperty("isTob")
    val isTob: Boolean = false,
    val players: MutableList<Player> = mutableListOf()
)
