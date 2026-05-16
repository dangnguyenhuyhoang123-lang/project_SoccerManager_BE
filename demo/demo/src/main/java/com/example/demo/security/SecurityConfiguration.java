package com.example.demo.security;

import com.example.demo.service.MyUserServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http,
//                                                   DaoAuthenticationProvider authenticationProvider) throws Exception {
//
//        http
//                .csrf(csrf -> csrf.disable())
//                .cors(Customizer.withDefaults())
//                .authenticationProvider(authenticationProvider)
//                .authorizeHttpRequests(auth -> {
//
//                    auth.requestMatchers(
//                                    "/api/user-account/login",
//                                    "/api/user-account/register"
//                            ).permitAll()
//                            .requestMatchers(HttpMethod.GET, "/api/user-account/me").authenticated();
//
//                    auth.requestMatchers(HttpMethod.GET,
//                            "/api/matches/**",
//                            "/api/player/**"
//                    ).permitAll();
//
//                    auth.requestMatchers("/api/user-account")
//                            .hasRole("ADMIN");
//
//                    auth.anyRequest().authenticated();
//                });
//
//        return http.build();
//    }



//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http.csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        // -------------------------------------------------------------
//                        // NHÓM 1: PUBLIC - Ai cũng xem được (STT: 4, 5, 6, 7)
//                        // Khán giả/Khách không cần đăng nhập vẫn gọi được
//                        // -------------------------------------------------------------
//                        .requestMatchers(HttpMethod.GET, "/api/players/**", "/api/clubs/**",
//                                "/api/matches/**", "/api/standings/**").permitAll()
//
//                        // -------------------------------------------------------------
//                        // NHÓM 2: DÙNG CHUNG ADMIN & TRỌNG TÀI (STT: 3)
//                        // Ghi nhận kết quả trận đấu
//                        // -------------------------------------------------------------
//                        .requestMatchers(HttpMethod.POST, "/api/matches/*/results").hasAnyRole("ADMIN", "REFEREE")
//                        .requestMatchers(HttpMethod.PUT, "/api/matches/*/results").hasAnyRole("ADMIN", "REFEREE")
//
//                        // -------------------------------------------------------------
//                        // NHÓM 3: DÙNG CHUNG ADMIN & QUẢN LÝ CLB (STT: 11, 12, 13)
//                        // Quản lý CLB, Cầu thủ, Sân bãi
//                        // -------------------------------------------------------------
//                        .requestMatchers("/api/management/clubs/**",
//                                "/api/management/players/**",
//                                "/api/management/stadiums/**").hasAnyRole("ADMIN", "CLUB_MANAGER")
//
//                        // -------------------------------------------------------------
//                        // NHÓM 4: CHỈ DÀNH RIÊNG CHO ADMIN (STT: 1, 2, 8, 9, 10, 14)
//                        // Quản lý giải, Vòng đấu, Xếp lịch, Đổi luật, Duyệt hồ sơ
//                        // -------------------------------------------------------------
//                        .requestMatchers("/api/admin/**",
//                                "/api/tournaments/**",
//                                "/api/rules/**",
//                                "/api/registrations/**").hasRole("ADMIN")
//
//                        .anyRequest().authenticated()
//                );
//        return http.build();
//    }
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll() // 🔥 mở toàn bộ
            );

    return http.build();
}
}
