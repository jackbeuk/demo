package com.example.demo

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class StartController {

    @GetMapping("/", produces = [MediaType.TEXT_HTML_VALUE])
    fun index(request: HttpServletRequest): ResponseEntity<Resource> {
        val host = request.getHeader("Host") ?: ""
        val resourcePath = when {
            host.contains("caruse.beukering.eu", ignoreCase = true) -> "static/caruse.html"
            host.contains("verjaardag.beukering.eu", ignoreCase = true) -> "static/verjaardag.html"
            else -> "static/index.html"
        }
        val resource = ClassPathResource(resourcePath)
        return if (resource.exists()) {
            ResponseEntity.ok(resource)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/login", produces = [MediaType.TEXT_HTML_VALUE])
    fun login(): ResponseEntity<Resource> {
        val resource = ClassPathResource("static/login.html")
        return if (resource.exists()) {
            ResponseEntity.ok(resource)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
