package com.authapi.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Called when OAuth2 login fails (user denied access, provider error, etc.)
 */
@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        log.error("OAuth2 authentication failed: {}", exception.getMessage());

        // Get the redirect URI from session to send error back to client app
        Object sessionRedirect = request.getSession().getAttribute("redirect_uri");
        request.getSession().removeAttribute("redirect_uri");

        String redirectUri = sessionRedirect != null ? sessionRedirect.toString() : null;

        if (redirectUri != null) {
            String targetUrl = redirectUri + "?error=authentication_failed&message="
                    + exception.getMessage();
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed: " + exception.getMessage());
        }
    }
}
