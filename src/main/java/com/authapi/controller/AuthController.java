package com.authapi.controller;

import com.authapi.model.AuthResponse;
import com.authapi.model.UserInfo;
import com.authapi.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;

    /**
     * Initiates OAuth2 login with a specific provider.
     *
     * Client app calls:
     *   GET /auth/login/{provider}?redirect_uri=https://clientapp.com/callback
     *
     * Supported providers: google, github, facebook
     *
     * The redirect_uri is stored in session and used after login to send the JWT back.
     */
    @GetMapping("/login/{provider}")
    public void initiateLogin(@PathVariable String provider,
                              @RequestParam(value = "redirect_uri", required = false) String redirectUri,
                              HttpServletRequest request,
                              HttpServletResponse response) throws IOException {

        // Store the client app's redirect URI in session for use after OAuth2 completes
        if (redirectUri != null && !redirectUri.isBlank()) {
            request.getSession().setAttribute("redirect_uri", redirectUri);
        }

        // Redirect to Spring Security's OAuth2 authorization endpoint
        response.sendRedirect("/oauth2/authorization/" + provider);
    }

    /**
     * Verifies a JWT token and returns the decoded user info.
     *
     * Client apps can use this to validate tokens server-side.
     *
     * POST /auth/verify
     * Body: { "token": "<jwt>" }
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
        }

        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
        }

        Claims claims = jwtService.validateAndGetClaims(token);

        UserInfo userInfo = UserInfo.builder()
                .email(claims.get("email", String.class))
                .name(claims.get("name", String.class))
                .picture(claims.get("picture", String.class))
                .provider(claims.get("provider", String.class))
                .id(claims.get("providerId", String.class))
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(userInfo)
                .build();

        return ResponseEntity.ok(authResponse);
    }

    /**
     * Health check endpoint.
     * GET /auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "auth-api",
                "providers", "google, github, facebook"
        ));
    }
}
