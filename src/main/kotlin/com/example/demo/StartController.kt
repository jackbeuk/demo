package com.example.demo

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class StartController {

    @GetMapping("/")
    fun index(): String = "forward:/index.html"

    @GetMapping("/login")
    fun login(): String = "forward:/login.html"
}
