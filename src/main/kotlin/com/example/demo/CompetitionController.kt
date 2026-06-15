package com.example.demo

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/competitions")
class CompetitionController(
    private val competitionRepository: CompetitionRepository,
    private val tennisController: TennisController
) {

    @GetMapping
    fun getCompetitions(): List<Competition> = competitionRepository.findAll()

    @PostMapping
    fun createCompetition(@RequestBody request: CreateCompetitionRequest): ResponseEntity<Competition> {
        if (competitionRepository.existsById(request.name)) {
            return ResponseEntity(HttpStatus.CONFLICT)
        }
        val competition = Competition(name = request.name)
        val saved = competitionRepository.save(competition)
        return ResponseEntity(saved, HttpStatus.CREATED)
    }

    @GetMapping("/{name}")
    fun getCompetition(@PathVariable name: String): ResponseEntity<Competition> {
        val competition = competitionRepository.findById(name).orElse(null)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        return ResponseEntity.ok(competition)
    }

    @DeleteMapping("/{name}")
    fun deleteCompetition(@PathVariable name: String): ResponseEntity<Void> {
        if (!competitionRepository.existsById(name)) return ResponseEntity(HttpStatus.NOT_FOUND)
        competitionRepository.deleteById(name)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @PostMapping("/{name}/teams")
    fun addTeam(
        @PathVariable name: String,
        @RequestBody request: AddTeamToCompetitionRequest
    ): ResponseEntity<Any> {
        val competition = competitionRepository.findById(name).orElse(null)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        if (competition.teams.size >= 10) {
            return ResponseEntity("Competitie zit vol (max 10 teams)", HttpStatus.BAD_REQUEST)
        }
        val team = tennisController.findTeam(request.teamId)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        if (competition.teams.any { it.id == team.id }) {
            return ResponseEntity("Team zit al in deze competitie", HttpStatus.CONFLICT)
        }
        if (team.isTob && competition.teams.any { it.isTob }) {
            return ResponseEntity("Er zit al een TOB team in deze competitie", HttpStatus.BAD_REQUEST)
        }
        if (!team.isTob && competition.teams.isEmpty()) {
            return ResponseEntity("Het eerste team moet een TOB team zijn", HttpStatus.BAD_REQUEST)
        }
        competition.teams.add(team)
        competitionRepository.save(competition)
        return ResponseEntity(competition, HttpStatus.OK)
    }

    @DeleteMapping("/{name}/teams/{teamId}")
    fun removeTeam(
        @PathVariable name: String,
        @PathVariable teamId: Long
    ): ResponseEntity<Any> {
        val competition = competitionRepository.findById(name).orElse(null)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val team = competition.teams.find { it.id == teamId }
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        if (team.isTob && competition.teams.size > 1) {
            return ResponseEntity("Verwijder eerst alle andere teams voordat je het TOB team verwijdert", HttpStatus.BAD_REQUEST)
        }
        competition.teams.removeIf { it.id == teamId }
        competitionRepository.save(competition)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @GetMapping("/{name}/speelronden")
    fun getSpeelronden(@PathVariable name: String): ResponseEntity<List<Speelronde>> {
        val competition = competitionRepository.findById(name).orElse(null)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        return ResponseEntity.ok(competition.speelronden)
    }

    @PostMapping("/{name}/speelronden")
    fun addSpeelronde(
        @PathVariable name: String,
        @RequestBody request: CreateSpeelrondeRequest
    ): ResponseEntity<Any> {
        val competition = competitionRepository.findById(name).orElse(null)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        if (!competition.teams.any { it.isTob }) {
            return ResponseEntity("Voeg eerst een TOB team toe aan de competitie", HttpStatus.BAD_REQUEST)
        }
        val tegenstander = tennisController.findTeam(request.tegenstander)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val maxSpeelrondeId = competition.speelronden.maxOfOrNull { it.id ?: 0L } ?: 0L
        val speelronde = Speelronde(
            id = maxSpeelrondeId + 1,
            locatie = request.locatie,
            datum = request.datum,
            tegenstander = tegenstander
        )
        competition.speelronden.add(speelronde)
        competitionRepository.save(competition)
        return ResponseEntity(speelronde, HttpStatus.CREATED)
    }

    @DeleteMapping("/{name}/speelronden/{id}")
    fun removeSpeelronde(
        @PathVariable name: String,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        val competition = competitionRepository.findById(name).orElse(null)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val removed = competition.speelronden.removeIf { it.id == id }
        if (removed) {
            competitionRepository.save(competition)
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        return ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @PostMapping("/{name}/speelronden/{id}/verhinderingen")
    fun setVerhindering(
        @PathVariable name: String,
        @PathVariable id: Long,
        @RequestBody request: SetVerhinderingRequest
    ): ResponseEntity<Any> {
        val competition = competitionRepository.findById(name).orElse(null)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val speelronde = competition.speelronden.find { it.id == id }
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val tobTeam = competition.teams.find { it.isTob }
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val player = tobTeam.players.find { it.id == request.playerId }
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        speelronde.verhinderingen.removeIf { it.playerId == request.playerId }
        val verhindering = Verhindering(
            playerId = player.id!!,
            playerName = player.name,
            beschikbaarheid = request.beschikbaarheid
        )
        speelronde.verhinderingen.add(verhindering)
        competitionRepository.save(competition)
        return ResponseEntity(verhindering, HttpStatus.OK)
    }

    @PostMapping("/{name}/plan")
    fun planCompetition(@PathVariable name: String): ResponseEntity<Any> {
        val competition = competitionRepository.findById(name).orElse(null)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val tobTeam = competition.teams.find { it.isTob }
            ?: return ResponseEntity("Geen TOB team in deze competitie", HttpStatus.BAD_REQUEST)

        // Initialize counts
        val playCounts = mutableMapOf<Long, Int>()
        val homeCounts = mutableMapOf<Long, Int>()
        val awayCounts = mutableMapOf<Long, Int>()
        tobTeam.players.forEach { p ->
            playCounts[p.id!!] = 0
            homeCounts[p.id!!] = 0
            awayCounts[p.id!!] = 0
        }

        // Clear previous assignments
        competition.speelronden.forEach { it.assignedPlayers.clear() }

        // Sort speelronden by date
        val sorted = competition.speelronden.sortedBy { it.datum }

        for (sr in sorted) {
            // Map of unavailable players for this speelronde (VERHINDERD = niet beschikbaar)
            val unavailable = sr.verhinderingen.filter { it.beschikbaarheid == Beschikbaarheid.VERHINDERD }.map { it.playerId }.toSet()
            // Players with VOORKEUR get priority in sorting
            val voorkeurIds = sr.verhinderingen.filter { it.beschikbaarheid == Beschikbaarheid.VOORKEUR }.map { it.playerId }.toSet()
            val availablePlayers = tobTeam.players.filter { it.id !in unavailable }

            // Split into regular and reserve players
            val regularPlayers = availablePlayers.filter { !it.isReserve }.map { it.id!! }.toMutableList()
            val reservePlayers = availablePlayers.filter { it.isReserve }.map { it.id!! }.toMutableList()

            // assign up to 4 players
            for (slot in 1..4) {
                // First try to assign from regular players
                var candidate = regularPlayers
                    .shuffled() // randomize order so planning differs each time
                    .sortedWith(compareBy<Long>(
                        { if (it in voorkeurIds) 0 else 1 }, // VOORKEUR players first
                        { playCounts[it] ?: 0 },
                        { id ->
                            if (sr.locatie == Locatie.THUIS) (homeCounts[id] ?: 0) - (awayCounts[id] ?: 0)
                            else (awayCounts[id] ?: 0) - (homeCounts[id] ?: 0)
                        }
                    ))
                    .firstOrNull()

                // If no regular players available, use reserve players
                if (candidate == null) {
                    candidate = reservePlayers
                        .shuffled() // randomize order so planning differs each time
                        .sortedWith(compareBy<Long>(
                            { if (it in voorkeurIds) 0 else 1 }, // VOORKEUR players first
                            { playCounts[it] ?: 0 },
                            { id ->
                                if (sr.locatie == Locatie.THUIS) (homeCounts[id] ?: 0) - (awayCounts[id] ?: 0)
                                else (awayCounts[id] ?: 0) - (homeCounts[id] ?: 0)
                            }
                        ))
                        .firstOrNull()
                }

                if (candidate == null) break

                sr.assignedPlayers.add(candidate)
                playCounts[candidate] = (playCounts[candidate] ?: 0) + 1
                if (sr.locatie == Locatie.THUIS) homeCounts[candidate] = (homeCounts[candidate] ?: 0) + 1
                else awayCounts[candidate] = (awayCounts[candidate] ?: 0) + 1

                // remove from available lists for this round so we don't pick same player twice
                regularPlayers.remove(candidate)
                reservePlayers.remove(candidate)
            }
        }

        competitionRepository.save(competition)
        return ResponseEntity(competition, HttpStatus.OK)
    }
}
