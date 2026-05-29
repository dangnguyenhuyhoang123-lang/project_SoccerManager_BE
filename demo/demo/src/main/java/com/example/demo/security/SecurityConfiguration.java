package com.example.demo.security;

import com.example.demo.service.MyUserServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(MyUserServiceImpl userService) {
        DaoAuthenticationProvider dap = new DaoAuthenticationProvider(userService);
        dap.setPasswordEncoder(passwordEncoder());
        return dap;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "https://*.ngrok-free.dev",
                "https://*.ngrok-free.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST","PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth

                                .requestMatchers("/ws/**").permitAll()
                        // Auth
                        .requestMatchers(
                                "/api/user-account/login",
                                "/api/user-account/register"
                        ).permitAll()

                                .requestMatchers(HttpMethod.PUT, "/api/user-account/*/info").permitAll()

                        .requestMatchers("/api/user-account/me").authenticated()
                        .requestMatchers("/api/user-account/**").hasRole("ADMIN")


                        // Crawler dev endpoints
                        .requestMatchers(
                                "/api/vleague/sync/**",
                                "/error",
                                "/api/standings/**"
                                ,"/api/team-stats/**",
                                "/api/matches/**",
                                "/api/player-stats/**"
                        ).permitAll()

//                        .requestMatchers(HttpMethod.GET, "/api/user-account/me").authenticated()
//                        .requestMatchers(HttpMethod.GET, "/api/user-account").hasRole("ADMIN")

                        // Public read APIs for website pages
                        .requestMatchers(HttpMethod.GET,
                                "/api/leagues/**",
                                "/api/seasons/**",
                                "/api/rounds/**",
                                "/api/teams/**",
                                "/api/stadiums/**",
                                "/api/matches/**",
                                "/api/standings/**",
                                "/api/player/getPlayer/**",
                                "/api/player/getAllPlayers",
                                "/api/coaches/**",
                                "/api/lineups/**",
                                "/api/news/**",
                                "/api/vleague/**",
                                "/api/vleague/sync/**",
                                "/api/vleague/sync/vpf-calendar/preview",
                                "/api/vleague/sync/vpf-calendar",
                                "/api/player-seasons/**",
                                "/api/system-rules/**"

                        ).permitAll()

                        // Registration workflow
                        .requestMatchers(HttpMethod.POST, "/api/registrations").hasAnyRole("ADMIN", "CLUB_MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/registrations/**").hasAnyRole("ADMIN", "CLUB_MANAGER")

                        .requestMatchers(HttpMethod.POST,
                                "/api/registrations/*/approve",
                                "/api/registrations/*/reject"
                        ).hasRole("ADMIN")


                        // Admin only management
                        .requestMatchers(HttpMethod.POST,
                                "/api/leagues/**",
                                "/api/seasons/**",
                                "/api/rounds/**",
                                "/api/system-rules/**",
                                "/api/season-teams/**",
                                "/api/player-seasons/PlayerSeason",
                                "/api/matches/addMatch"
                        ).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/leagues/**",
                                "/api/seasons/**",
                                "/api/rounds/**",
                                "/api/system-rules/**",
                                "/api/season-teams/**",
                                "/api/player-seasons/updatePlayerSeason/**",
                                "/api/matches/updateMatch/**"
                        ).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/leagues/**",
                                "/api/seasons/**",
                                "/api/rounds/**",
                                "/api/system-rules/**",
                                "/api/season-teams/**",
                                "/api/player-seasons/deletePlayerSeason/**",
                                "/api/matches/deleteMatch/**",
                                "/api/vleague/sync/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PATCH, "/api/matches/*/status").hasRole("ADMIN")

                        // Admin and club manager shared management
                        .requestMatchers(HttpMethod.POST,
                                "/api/teams/**",
                                "/api/stadiums/**",
                                "/api/coaches/**",
                                "/api/player/addPlayer",
                                "/api/season-team-coaches/**",
                                "/api/lineups/submit"
                        ).hasAnyRole("ADMIN", "CLUB_MANAGER")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/teams/**",
                                "/api/stadiums/**",
                                "/api/coaches/**",
                                "/api/player/updatePlayer/**",
                                "/api/season-team-coaches/**"
                        ).hasAnyRole("ADMIN", "CLUB_MANAGER")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/teams/**",
                                "/api/stadiums/**",
                                "/api/coaches/**",
                                "/api/player/deletePlayer/**",
                                "/api/season-team-coaches/**",
                                "/api/lineups/match/**"
                        ).hasAnyRole("ADMIN", "CLUB_MANAGER")

                        // Club workspace read APIs
                        .requestMatchers(HttpMethod.GET,
                                "/api/season-teams",
                                "/api/season-team-coaches/**",
                                "/api/player-seasons/**",
                                "/api/player/getPlayersByTeam/**"
                        ).hasAnyRole("ADMIN", "CLUB_MANAGER")
//                        system rule
                                .requestMatchers(HttpMethod.GET, "/api/system-rules/**").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/system-rules/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/system-rules/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/system-rules/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);



        return http.build();
    }
}
