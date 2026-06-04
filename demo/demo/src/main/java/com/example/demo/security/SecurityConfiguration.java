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
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth

                        // =========================
                        // WebSocket
                        // =========================
                        .requestMatchers("/ws/**").permitAll()

                        // =========================
                        // Auth
                        // =========================
                        .requestMatchers(HttpMethod.POST,
                                "/api/user-account/login",
                                "/api/user-account/register"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/user-account/me")
                        .authenticated()

                        // User tự cập nhật thông tin cá nhân.
                        // Không nên permitAll.
                        .requestMatchers(HttpMethod.PUT, "/api/user-account/*/info")
                        .authenticated()

                        // Admin quản lý tài khoản
                        .requestMatchers("/api/user-account/**")
                        .hasRole("ADMIN")

                        // =========================
                        // PUBLIC READ APIs
                        // Chỉ GET mới public.
                        // =========================
                        .requestMatchers(HttpMethod.GET,
                                "/api/leagues/**",
                                "/api/seasons/**",
                                "/api/rounds/**",
                                "/api/teams/**",
                                "/api/stadiums/**",
                                "/api/matches/**",
                                "/api/standings/**",
                                "/api/team-stats/**",
                                "/api/player-stats/**",
                                "/api/player/**",
                                "/api/coaches/**",
                                "/api/news/**",
                                "/api/referees/**",
                                "/api/match-referees/**",
                                "/api/lineups/**",
                                "/api/vleague/**"
                        ).permitAll()

                        // Nếu public page cần xem player-season thì cho GET public.
                        // Nếu bạn thấy lộ dữ liệu quá nhiều, đổi thành authenticated.
                        .requestMatchers(HttpMethod.GET,
                                "/api/player-seasons/**",
                                "/api/season-teams/**",
                                "/api/season-team-coaches/**"
                        ).permitAll()

                        // =========================
                        // CRAWLER / SYNC
                        // Chỉ ADMIN được crawl/sync dữ liệu.
                        // Không nên permitAll.
                        // =========================
                        .requestMatchers("/api/vleague/sync/**")
                        .hasRole("ADMIN")

                        // =========================
                        // SYSTEM RULES
                        // Public có thể xem luật đang áp dụng nếu cần hiển thị.
                        // Tạo/sửa/xóa chỉ Admin.
                        // =========================
                        .requestMatchers(HttpMethod.GET, "/api/system-rules/**")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/system-rules/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/system-rules/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/api/system-rules/**")
                        .hasRole("ADMIN")

                        // =========================
                        // REGISTRATION WORKFLOW
                        // =========================

                        // CLB gửi hồ sơ, Admin cũng có thể tạo/hỗ trợ nếu cần
                        .requestMatchers(HttpMethod.POST,
                                "/api/registrations",
                                "/api/registrations/**"
                        ).hasAnyRole("ADMIN", "CLUB_MANAGER")

                        // Admin xem tất cả, Club Manager xem hồ sơ của CLB mình.
                        // Phân quyền chi tiết nên kiểm tra thêm trong service.
                        .requestMatchers(HttpMethod.GET, "/api/registrations/**")
                        .hasAnyRole("ADMIN", "CLUB_MANAGER")

                        // Duyệt/từ chối chỉ Admin
                        .requestMatchers(HttpMethod.POST,
                                "/api/registrations/*/approve",
                                "/api/registrations/*/reject"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/registrations/**")
                        .hasAnyRole("ADMIN", "CLUB_MANAGER")

                        .requestMatchers(HttpMethod.DELETE, "/api/registrations/**")
                        .hasRole("ADMIN")

                        // =========================
                        // INVITATIONS
                        // Nếu project có season invitation.
                        // =========================
                        .requestMatchers(HttpMethod.GET, "/api/season-invitations/**")
                        .hasAnyRole("ADMIN", "CLUB_MANAGER")

                        .requestMatchers(HttpMethod.POST, "/api/season-invitations/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/season-invitations/**")
                        .hasAnyRole("ADMIN", "CLUB_MANAGER")

                        .requestMatchers(HttpMethod.DELETE, "/api/season-invitations/**")
                        .hasRole("ADMIN")

                        // =========================
                        // ADMIN MANAGEMENT
                        // =========================

                        // League / Season / Round
                        .requestMatchers(HttpMethod.POST,
                                "/api/leagues/**",
                                "/api/seasons/**",
                                "/api/rounds/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/leagues/**",
                                "/api/seasons/**",
                                "/api/rounds/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/leagues/**",
                                "/api/seasons/**",
                                "/api/rounds/**"
                        ).hasRole("ADMIN")

                        // Season team / player season / coach season
                        .requestMatchers(HttpMethod.POST,
                                "/api/season-teams/**",
                                "/api/player-seasons/**",
                                "/api/season-team-coaches/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/season-teams/**",
                                "/api/player-seasons/**",
                                "/api/season-team-coaches/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/season-teams/**",
                                "/api/player-seasons/**",
                                "/api/season-team-coaches/**"
                        ).hasRole("ADMIN")

                        // Match management
                        .requestMatchers(HttpMethod.POST,
                                "/api/matches/addMatch",
                                "/api/matches/generate-schedule/**",
                                "/api/matches/*/predict"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/matches/updateMatch/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/matches/*/status"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/matches/deleteMatch/**"
                        ).hasRole("ADMIN")

                        // Match result/events/stats thường Admin cập nhật
                        .requestMatchers(HttpMethod.POST,
                                "/api/matches/*/events/**",
                                "/api/matches/*/stats/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/matches/*/events/**",
                                "/api/matches/*/stats/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/matches/*/events/**",
                                "/api/matches/*/stats/**"
                        ).hasRole("ADMIN")

                        // =========================
                        // REFEREE
                        // Public GET, Admin quản lý/phân công.
                        // =========================
                        .requestMatchers(HttpMethod.POST,
                                "/api/referees/**",
                                "/api/match-referees/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/referees/**",
                                "/api/match-referees/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/referees/**",
                                "/api/match-referees/**"
                        ).hasRole("ADMIN")

                        // =========================
                        // TEAM / PLAYER / COACH / STADIUM
                        // Admin và Club Manager cùng quản lý.
                        // Nhưng service phải kiểm tra Club Manager chỉ thao tác team của mình.
                        // =========================
                        .requestMatchers(HttpMethod.POST,
                                "/api/teams/**",
                                "/api/stadiums/**",
                                "/api/coaches/**",
                                "/api/player/addPlayer"
                        ).hasAnyRole("ADMIN", "CLUB_MANAGER")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/teams/**",
                                "/api/stadiums/**",
                                "/api/coaches/**",
                                "/api/player/updatePlayer/**"
                        ).hasAnyRole("ADMIN", "CLUB_MANAGER")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/teams/**",
                                "/api/stadiums/**",
                                "/api/coaches/**",
                                "/api/player/deletePlayer/**"
                        ).hasAnyRole("ADMIN", "CLUB_MANAGER")

                        // =========================
                        // LINEUP
                        // CLB nộp đội hình, Admin có thể xem/quản lý.
                        // =========================
                        .requestMatchers(HttpMethod.POST,
                                "/api/lineups/submit",
                                "/api/matches/*/teams/*/lineup"
                        ).hasAnyRole("ADMIN", "CLUB_MANAGER")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/lineups/**",
                                "/api/matches/*/teams/*/lineup"
                        ).hasAnyRole("ADMIN", "CLUB_MANAGER")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/lineups/**"
                        ).hasAnyRole("ADMIN", "CLUB_MANAGER")

                        // =========================
                        // Fallback
                        // =========================
                        .anyRequest().authenticated()
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
