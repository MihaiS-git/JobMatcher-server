package com.jobmatcher.server.config;

import com.jobmatcher.server.security.CustomOAuth2FailureHandler;
import com.jobmatcher.server.security.CustomOAuth2SuccessHandler;
import com.jobmatcher.server.security.JwtAuthenticationFilter;
import com.jobmatcher.server.security.RateLimitingFilter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Value("${frontend.url.prod}")
    private String FRONTEND_URL_PROD;

    @Value("${frontend.url.dev}")
    private String FRONTEND_URL_DEV;

    @Value("${frontend.url.built}")
    private String FRONTEND_URL_BUILT;


    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
    private final CustomOAuth2FailureHandler customOAuth2FailureHandler;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    public SecurityConfiguration(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitingFilter rateLimitingFilter,
            CustomOAuth2SuccessHandler customOAuth2SuccessHandler,
            CustomOAuth2FailureHandler customOAuth2FailureHandler,
            CustomAccessDeniedHandler customAccessDeniedHandler,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.customOAuth2SuccessHandler = customOAuth2SuccessHandler;
        this.customOAuth2FailureHandler = customOAuth2FailureHandler;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                FRONTEND_URL_PROD,
                FRONTEND_URL_DEV,
                FRONTEND_URL_BUILT
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public UserDetailsService prometheusUserDetailsService(
            @Value("${spring.security.user.name}") String username,
            @Value("${spring.security.user.password}") String password
    ) {
        var user = User.withUsername(username)
                .password(new BCryptPasswordEncoder().encode(password))
                .roles("PROMETHEUS")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                // Configure public endpoints
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                API_VERSION + "/auth/register",
                                API_VERSION + "/auth/login",
                                API_VERSION + "/auth/recover-password",
                                API_VERSION + "/auth/validate-reset-token",
                                API_VERSION + "/auth/reset-password",
                                API_VERSION + "/auth/refresh-token",
                                API_VERSION + "/payments/stripe/webhook",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/error"
                        ).permitAll()
                        // Actuator Prometheus uses HTTP Basic Auth
                        .requestMatchers( "/actuator/prometheus").hasRole("PROMETHEUS")
                        // All other actuator endpoints require JWT/OAuth2
                        .requestMatchers("/actuator/**").authenticated()
                        // All other endpoints require JWT/OAuth2
                        .anyRequest().authenticated())
                // Enable HTTP Basic only for Prometheus scraping
                .httpBasic(Customizer.withDefaults())
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(customOAuth2SuccessHandler)
                        .failureHandler(customOAuth2FailureHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, RateLimitingFilter.class);

        return http.build();
    }

//    @PostConstruct
//    public void printUser() {
//        System.out.println("Prometheus user: " + System.getenv("SPRING_SECURITY_USER_NAME"));
//        System.out.println("Prometheus password: " + System.getenv("SPRING_SECURITY_USER_PASSWORD"));
//    }

}
