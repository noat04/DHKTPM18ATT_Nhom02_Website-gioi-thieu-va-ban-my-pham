package org.fit.shopnuochoa.config;

import org.fit.shopnuochoa.component.CustomSuccessHandler;
import org.fit.shopnuochoa.service.CustomUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Cho phép dùng @PreAuthorize
public class SecurityConfig {
    private final CustomSuccessHandler successHandler;
    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService, CustomSuccessHandler successHandler) {
        this.customUserDetailsService = customUserDetailsService;
        this.successHandler = successHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/consultant/**") // Tắt CSRF cho API consultant chat
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/register",
                                "/api/login",
                                "/api/products/**",
                                "/api/products/detail/**",
                                "/api/categories/**",
                                "/api/consultant/**",
                                "/api/images/**",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll() // 👈 các trang công khai
                        .requestMatchers(
                                "/api/cart/**",
                                "/api/customers/**",
                                "/api/orders/**",
                                "/api/comments/add",
                                "/api/comments/edit", // <-- [THÊM MỚI]
                                "/api/comments/delete/**", // <-- [THÊM MỚI]
                                "/api/wishlist/toggle",
                                "/api/profile/**"
                                ).authenticated() // 👈 các trang cần login
                        .anyRequest().permitAll()

                )
                .formLogin(form -> form
                        .loginPage("/api/login")
                        .successHandler(successHandler)  // 👈 Dùng custom handler// Trang login của bạn
                        .loginProcessingUrl("/api/login")   // URL POST form
//                        .defaultSuccessUrl("/api/home", true) // 👈 Chuyển hướng về đây sau khi login thành công
                        .failureUrl("/api/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessUrl("/api/login?logout=true")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .authenticationProvider(authenticationProvider());
        return http.build();
    }
}

