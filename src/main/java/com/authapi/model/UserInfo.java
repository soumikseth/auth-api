package com.authapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the authenticated user's profile extracted from the OAuth2 provider.
 * This is NOT persisted — it's used to build the JWT and return to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private String id;          // Provider's user ID
    private String email;
    private String name;
    private String picture;     // Avatar URL
    private String provider;    // "google", "github", "facebook"
}
