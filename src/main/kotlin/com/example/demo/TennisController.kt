package com.example.demo

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/teams")
class TennisController(private val teamRepository: TeamRepository) {

    @GetMapping
    fun getTeams(): List<Team> = teamRepository.findAll()

    @PostMapping
    fun createTeam(@RequestBody request: CreateTeamRequest): ResponseEntity<Team> {
        val maxId = teamRepository.findAll().maxOfOrNull { it.id ?: 0L } ?: 0L
        val team = Team(id = maxId + 1, name = request.name, isTob = request.isTob)
        val saved = teamRepository.save(team)
        return ResponseEntity(saved, HttpStatus.CREATED)
    }

    @GetMapping("/{teamId}")
    fun getTeam(@PathVariable teamId: Long): ResponseEntity<Team> {
        val team = teamRepository.findById(teamId).orElse(null) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        return ResponseEntity.ok(team)
    }

    @DeleteMapping("/{teamId}")
    fun deleteTeam(@PathVariable teamId: Long): ResponseEntity<Void> {
        if (!teamRepository.existsById(teamId)) return ResponseEntity(HttpStatus.NOT_FOUND)
        teamRepository.deleteById(teamId)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @GetMapping("/{teamId}/players")
    fun getPlayers(@PathVariable teamId: Long): ResponseEntity<List<Player>> {
        val team = teamRepository.findById(teamId).orElse(null) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        return ResponseEntity.ok(team.players)
    }

    @PostMapping("/{teamId}/players")
    fun addPlayer(
        @PathVariable teamId: Long,
        @RequestBody request: CreatePlayerRequest
    ): ResponseEntity<Player> {
        val team = teamRepository.findById(teamId).orElse(null) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val maxPlayerId = team.players.maxOfOrNull { it.id ?: 0L } ?: 0L
        val player = Player(
            id = maxPlayerId + 1,
            name = request.name,
            ranking = request.ranking,
            isReserve = request.isReserve
        )
        team.players.add(player)
        teamRepository.save(team)
        return ResponseEntity(player, HttpStatus.CREATED)
    }

    fun findTeam(id: Long): Team? = teamRepository.findById(id).orElse(null)

    @DeleteMapping("/{teamId}/players/{playerId}")
    fun removePlayer(
        @PathVariable teamId: Long,
        @PathVariable playerId: Long
    ): ResponseEntity<Void> {
        val team = teamRepository.findById(teamId).orElse(null) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val removed = team.players.removeIf { it.id == playerId }
        if (removed) {
            teamRepository.save(team)
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        return ResponseEntity(HttpStatus.NOT_FOUND)
    }
}

data class CreateTeamRequest(val name: String, val isTob: Boolean = false)
data class CreatePlayerRequest(val name: String, val ranking: Int, val isReserve: Boolean = false)
