package com.example.demo

import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.RequestMatcher

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(PublicHostMatcher()).permitAll()
                    .requestMatchers("/caruse.html", "/verjaardag.html").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().authenticated()
            }
            .formLogin { form ->
                form
                    .defaultSuccessUrl("/", true)
                    .permitAll()
            }
            .logout { logout ->
                logout
                    .logoutSuccessUrl("/login?logout")
                    .permitAll()
            }
        return http.build()
    }

    @Bean
    fun userDetailsService(): UserDetailsService {
        val admin = User.withDefaultPasswordEncoder()
            .username("admin")
            .password("admin123")
            .roles("ADMIN")
            .build()

        val user = User.withDefaultPasswordEncoder()
            .username("user")
            .password("user123")
            .roles("USER")
            .build()

        return InMemoryUserDetailsManager(admin, user)
    }

    /**
     * Matcher that permits requests without authentication when the Host header
     * contains caruse.beukering.eu or verjaardag.beukering.eu.
     */
    class PublicHostMatcher : RequestMatcher {
        private val publicHosts = listOf("caruse.beukering.eu", "verjaardag.beukering.eu")

        override fun matches(request: HttpServletRequest): Boolean {
            val host = request.getHeader("Host") ?: return false
            return publicHosts.any { host.contains(it, ignoreCase = true) }
        }
    }
}
