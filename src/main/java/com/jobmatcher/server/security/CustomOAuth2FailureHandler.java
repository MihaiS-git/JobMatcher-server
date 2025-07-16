package com.jobmatcher.server.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class CustomOAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${frontend.base.url}")
    private String FRONTEND_BASE_URL;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        exception.printStackTrace();

        String redirectUri = UriComponentsBuilder
                .fromUriString(FRONTEND_BASE_URL + "/oauth2/redirect")
                .queryParam("error", exception.getLocalizedMessage())
                .build()
                .toUriString();

        response.sendRedirect(redirectUri);
    }
}
