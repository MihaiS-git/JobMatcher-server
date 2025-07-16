package com.jobmatcher.server.security;

import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.InvalidAuthException;
import com.jobmatcher.server.repository.UserRepository;
import com.jobmatcher.server.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/api/v0/auth/reset-password") || requestURI.startsWith("/api/v0/auth/recover-password")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            if (jwtService.isTokenValid(jwt)) {
                String email = jwtService.extractUsername(jwt);
                User user = userRepository.findByEmail(email).orElseThrow(() -> new InvalidAuthException("USer not found"));

                var authorities = AuthorityUtils.createAuthorityList("ROLE_" + user.getRole().name());

                var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                // Token invalid: respond with 401
                handleUnauthorized(response, "Invalid JWT token");
            }
        } catch (InvalidAuthException e) {
            SecurityContextHolder.clearContext();
            handleUnauthorized(response, e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void handleUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        String json = "{\"error\": \"" + message + "\"}";
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}
