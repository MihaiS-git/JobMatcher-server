package com.jobmatcher.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 60;        // max requests per window
    private static final long WINDOW_MS = 60_000;       // 1 minute window

    private final Map<String, SlidingWindow> userRequests = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Bypass rate limiting for localhost (development)
        if ("127.0.0.1".equals(request.getRemoteAddr())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Log each request for monitoring
        log.info("🔥 RateLimitingFilter executing for: {}", request.getRequestURI());

        String userKey = request.getRemoteAddr(); // fallback to IP if anonymous

        // optionally extract username from JWT if available
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            userKey = authHeader.substring(7); // simple token key; can be username
        }

        long now = System.currentTimeMillis();
        SlidingWindow window = userRequests.computeIfAbsent(userKey, k -> new SlidingWindow(0, now));

        synchronized (window) {
            if (now - window.startTime >= WINDOW_MS) {
                window.startTime = now;
                window.count = 0;
            }

            window.count++;
            if (window.count > MAX_REQUESTS) {
                System.out.println("RATE LIMIT TRIGGERED for key: " + userKey);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static class SlidingWindow {
        int count;
        long startTime;

        SlidingWindow(int count, long startTime) {
            this.count = count;
            this.startTime = startTime;
        }
    }
}
