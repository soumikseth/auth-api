package com.authapi.handler;

import com.authapi.config.AppProperties;
import com.authapi.model.UserInfo;
import com.authapi.service.JwtService;
import com.authapi.service.OAuth2UserInfoExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Called after a successful OAuth2 login.
 * Extracts user info, generates a JWT, and redirects back to the client app.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final OAuth2UserInfoExtractor userInfoExtractor;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        String provider = authToken.getAuthorizedClientRegistrationId(); // "google", "github", etc.
        OAuth2User oAuth2User = authToken.getPrincipal();

        // Extract normalized user info from provider-specific attributes
        UserInfo userInfo = userInfoExtractor.extract(provider, oAuth2User);
        log.info("OAuth2 login success: provider={}, email={}", provider, userInfo.getEmail());

        // Generate JWT
        String jwt = jwtService.generateToken(userInfo);

        // Get the redirect URI the client app originally requested (stored in session)
        String redirectUri = getRedirectUri(request);

        if (!isAllowedRedirectUri(redirectUri)) {
            log.warn("Blocked redirect to non-whitelisted URI: {}", redirectUri);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Redirect URI not allowed");
            return;
        }

        // Build final redirect URL with token as query param
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", jwt)
                .queryParam("provider", provider)
                .build().toUriString();

        log.debug("Redirecting to: {}", redirectUri); // don't log the token
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Retrieves the redirect_uri the client app passed before the OAuth2 flow started.
     * Falls back to the first allowed URI if none was stored.
     */
    private String getRedirectUri(HttpServletRequest request) {
        // Check session attribute set by AuthController
        Object sessionRedirect = request.getSession().getAttribute("redirect_uri");
        if (sessionRedirect != null) {
            request.getSession().removeAttribute("redirect_uri");
            return sessionRedirect.toString();
        }
        List<String> allowedRedirectUris = appProperties.getAllowedRedirectUris();
        return allowedRedirectUris == null || allowedRedirectUris.isEmpty() ? "/" : allowedRedirectUris.get(0);
    }

    private boolean isAllowedRedirectUri(String redirectUri) {
        try {
            URI uri = URI.create(redirectUri);
            return appProperties.getAllowedRedirectUris().stream().anyMatch(allowed -> {
                try {
                    URI allowedUri = URI.create(allowed);
                    return allowedUri.getHost().equalsIgnoreCase(uri.getHost())
                            && allowedUri.getPort() == uri.getPort()
                            && uri.getPath().startsWith(allowedUri.getPath());
                } catch (Exception e) {
                    return false;
                }
            });
        } catch (Exception e) {
            return false;
        }
    }
}
