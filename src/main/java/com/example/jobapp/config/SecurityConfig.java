package com.example.jobapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment) throws Exception {
        boolean devProfile = environment.acceptsProfiles(Profiles.of("dev"));
        boolean listPreview = Boolean.TRUE.equals(
                environment.getProperty("jobapp.preview-list-without-auth", Boolean.class, false));
        boolean allowUnauthenticatedList = devProfile || listPreview;
        return http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/register")).permitAll();
                    auth.requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/register")).permitAll();
                    auth.requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll();
                    if (allowUnauthenticatedList) {
                        auth.requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/applications")).permitAll();
                        auth.requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/applications/")).permitAll();
                        auth.requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/applications/api/applications")).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/applications", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .build();
    }
}

