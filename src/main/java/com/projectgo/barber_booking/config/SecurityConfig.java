package com.projectgo.barber_booking.config;

import com.projectgo.barber_booking.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService uds;

    public SecurityConfig(CustomUserDetailsService uds) {
        this.uds = uds;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider prov = new DaoAuthenticationProvider();
        prov.setUserDetailsService(uds);
        prov.setPasswordEncoder(passwordEncoder());
        return prov;
    }

    /** เด้งไป /admin ถ้า ROLE_ADMIN ไม่งั้น /home */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SavedRequestAwareAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth)
                    throws IOException, ServletException {
                boolean isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                String target = isAdmin ? "/admin" : "/home";
                getRedirectStrategy().sendRedirect(req, res, target);
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ✅ ยกเว้น CSRF เฉพาะ endpoint ที่เราต้องโพสต์ JSON จากหน้าเว็บ
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                    new AntPathRequestMatcher("/api/reviews",   "POST"),
                    new AntPathRequestMatcher("/api/bookings",  "POST")
            ))

            .authorizeHttpRequests(auth -> auth
                // ✅ สแตติกไฟล์
                .requestMatchers(
                        "/css/**", "/js/**", "/images/**", "/webjars/**",
                        "/fonts/**", "/uploads/**", "/favicon.ico"
                ).permitAll()

                // ✅ หน้า public
                .requestMatchers("/", "/home", "/select_service",
                                 "/login", "/register", "/perform_login",
                                 "/error").permitAll()

                // ✅ อ่านเรตติ้ง/รีวิวบริการ (public)
                .requestMatchers(
                        new AntPathRequestMatcher("/api/services/*/rating",  "GET"),
                        new AntPathRequestMatcher("/api/services/*/reviews", "GET")
                ).permitAll()

                // ✅ ลูกค้าสร้างการจองได้ (public)
                .requestMatchers(new AntPathRequestMatcher("/api/bookings", "POST")).permitAll()

                // ✅ หน้าฟอร์มรีวิว เข้าถึงได้เมื่อเข้าสู่ระบบ (ให้สิทธิ์ตรวจซ้ำใน Controller แล้ว)
                .requestMatchers("/reviews/new").authenticated()

                // ✅ ฝั่งแอดมิน
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")

                // ✅ อื่น ๆ ต้องล็อกอิน (เช่น /booking/**, /profile/** เป็นต้น)
                .anyRequest().authenticated()
            )

            .authenticationProvider(authenticationProvider())

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/login?error")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/logout")                 // ใช้ POST /logout (ปลอดภัยสุด)
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}
