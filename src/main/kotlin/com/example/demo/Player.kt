package com.example.demo

import org.springframework.data.annotation.Id

data class Player(
    @Id
    val id: Long? = null,
    val name: String,
    val ranking: Int,
    val isReserve: Boolean = false
)
