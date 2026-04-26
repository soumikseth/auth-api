package com.authapi.service;

import com.authapi.model.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Extracts a normalized UserInfo from different OAuth2 providers.
 * Each provider returns attributes in a different format — this normalizes them.
 */
@Slf4j
@Service
public class OAuth2UserInfoExtractor {

    public UserInfo extract(String provider, OAuth2User oAuth2User) {
        return switch (provider.toLowerCase()) {
            case "google"   -> extractFromGoogle(oAuth2User);
            case "github"   -> extractFromGithub(oAuth2User);
            case "facebook" -> extractFromFacebook(oAuth2User);
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }

    // -------------------------------------------------------------------------
    // Google: returns sub, email, name, picture directly
    // -------------------------------------------------------------------------
    private UserInfo extractFromGoogle(OAuth2User user) {
        Map<String, Object> attrs = user.getAttributes();
        return UserInfo.builder()
                .id(String.valueOf(attrs.get("sub")))
                .email(String.valueOf(attrs.get("email")))
                .name(String.valueOf(attrs.get("name")))
                .picture(String.valueOf(attrs.getOrDefault("picture", "")))
                .provider("google")
                .build();
    }

    // -------------------------------------------------------------------------
    // GitHub: login, id, name, email (email may be null if private — handle it)
    // -------------------------------------------------------------------------
    private UserInfo extractFromGithub(OAuth2User user) {
        Map<String, Object> attrs = user.getAttributes();

        String email = attrs.get("email") != null
                ? String.valueOf(attrs.get("email"))
                : attrs.get("login") + "@github.noemail"; // fallback if email is private

        String avatar = attrs.get("avatar_url") != null
                ? String.valueOf(attrs.get("avatar_url"))
                : "";

        String name = attrs.get("name") != null
                ? String.valueOf(attrs.get("name"))
                : String.valueOf(attrs.get("login")); // fallback to username

        return UserInfo.builder()
                .id(String.valueOf(attrs.get("id")))
                .email(email)
                .name(name)
                .picture(avatar)
                .provider("github")
                .build();
    }

    // -------------------------------------------------------------------------
    // Facebook: id, name, email, picture.data.url
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private UserInfo extractFromFacebook(OAuth2User user) {
        Map<String, Object> attrs = user.getAttributes();

        String picture = "";
        Object pictureObj = attrs.get("picture");
        if (pictureObj instanceof Map) {
            Map<String, Object> pictureMap = (Map<String, Object>) pictureObj;
            Object data = pictureMap.get("data");
            if (data instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) data;
                Object url = dataMap.get("url");
                picture = url != null ? url.toString() : "";
            }
        }

        return UserInfo.builder()
                .id(String.valueOf(attrs.get("id")))
                .email(String.valueOf(attrs.getOrDefault("email", "")))
                .name(String.valueOf(attrs.get("name")))
                .picture(picture)
                .provider("facebook")
                .build();
    }
}
