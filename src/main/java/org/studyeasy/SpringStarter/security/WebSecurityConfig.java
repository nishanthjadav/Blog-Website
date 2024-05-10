package org.studyeasy.SpringStarter.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.studyeasy.SpringStarter.util.constants.Privileges;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {
        private static final String[] WHITELIST = {
                        "/",
                        "/login",
                        "/register",
                        "/db-console/**",
                        "/css/**",
                        "/fonts/**",
                        "/images/**",
                        "/js/**",
                        "/posts/**",
                        "/forgot-password",
                        "/reset-password",
                        "/change-password/**",
                        "/api/v1/**",

        };

        @Bean
        public static PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @SuppressWarnings("removal")
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(WHITELIST).permitAll()
                        .requestMatchers("/profile/**").authenticated()
                        .requestMatchers("/update_photo/**").authenticated()
                        .requestMatchers("/posts/add/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/editor/**").hasAnyRole("ADMIN", "EDITOR")
                        .requestMatchers("/admin/**").hasAuthority(Privileges.ACCESS_ADMIN_PANEL.getPrivilege()))
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/"))
                                .rememberMe(rememberMe -> rememberMe
                                                .rememberMeParameter("remember-me"))
                .httpBasic();

                // REMOVE THESE AFTER UPGRADING THE DB FROM H2 INFILE DB
                http.csrf(csrf -> csrf.disable());
                http.headers(headers -> headers.frameOptions().disable());

                return http.build();
        }
}
