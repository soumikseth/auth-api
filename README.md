# Auth API — Plug-and-Play OAuth2 Login Service

A stateless Spring Boot auth service. Drop this in, configure your OAuth2 credentials, and any client app can get Google/GitHub/Facebook login with a JWT in return. No database required.

---

## How It Works

```
Client App                    Auth API                      Google/GitHub/Facebook
    |                             |                                    |
    |-- GET /auth/login/google -->|                                    |
    |   ?redirect_uri=...         |-- redirect to provider ----------->|
    |                             |<-- user logs in -------------------|
    |                             |-- extract profile, sign JWT        |
    |<-- redirect with ?token=JWT-|                                    |
    |                             |                                    |
    |-- POST /auth/verify ------->|                                    |
    |<-- { user info } -----------|                                    |
```

---

## Quick Start

### 1. Configure OAuth2 Credentials

Edit `src/main/resources/application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET
          github:
            client-id: YOUR_GITHUB_CLIENT_ID
            client-secret: YOUR_GITHUB_CLIENT_SECRET
          facebook:
            client-id: YOUR_FACEBOOK_APP_ID
            client-secret: YOUR_FACEBOOK_APP_SECRET
```

Also set your JWT secret and allowed redirect URIs:

```yaml
app:
  jwt:
    secret: "your-strong-random-secret-min-32-chars"
  allowed-redirect-uris:
    - "https://yourclientapp.com/callback"
```

### 2. Set Up OAuth2 Apps

**Google:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a project → APIs & Services → Credentials → OAuth 2.0 Client ID
3. Add authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`

**GitHub:**
1. Go to GitHub → Settings → Developer Settings → OAuth Apps → New OAuth App
2. Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`

**Facebook:**
1. Go to [Meta for Developers](https://developers.facebook.com/)
2. Create App → Facebook Login → Settings
3. Valid OAuth Redirect URI: `http://localhost:8080/login/oauth2/code/facebook`

### 3. Run

```bash
cd auth-api
mvn spring-boot:run
```

---

## API Endpoints

### `GET /auth/login/{provider}`

Initiates OAuth2 login. Redirect your user to this URL.

| Parameter      | Required | Description                                      |
|----------------|----------|--------------------------------------------------|
| `provider`     | Yes      | `google`, `github`, or `facebook`                |
| `redirect_uri` | No       | Where to send the JWT after login                |

**Example:**
```
GET http://localhost:8080/auth/login/google?redirect_uri=https://myapp.com/callback
```

After login, the user is redirected to:
```
https://myapp.com/callback?token=eyJhbGci...&provider=google
```

---

### `POST /auth/verify`

Validates a JWT and returns the decoded user info. Use this server-side to verify tokens.

**Request:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response:**
```json
{
  "token": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "1234567890",
    "email": "user@gmail.com",
    "name": "Soumik Seth",
    "picture": "https://lh3.googleusercontent.com/...",
    "provider": "google"
  }
}
```

---

### `GET /auth/health`

```json
{
  "status": "UP",
  "service": "auth-api",
  "providers": "google, github, facebook"
}
```

---

## Client App Integration (Example)

### React / Next.js

```javascript
// 1. Redirect user to login
const login = (provider) => {
  window.location.href = `http://localhost:8080/auth/login/${provider}?redirect_uri=${window.location.origin}/callback`;
};

// 2. On your /callback page, grab the token
const params = new URLSearchParams(window.location.search);
const token = params.get('token');
localStorage.setItem('auth_token', token);

// 3. Verify token on your backend (optional but recommended)
const res = await fetch('http://localhost:8080/auth/verify', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ token })
});
const { user } = await res.json();
```

### Plain HTML

```html
<a href="http://localhost:8080/auth/login/google?redirect_uri=http://localhost:3000/callback">
  Login with Google
</a>
```

---

## JWT Payload

The JWT contains:

```json
{
  "sub": "user@gmail.com",
  "email": "user@gmail.com",
  "name": "Soumik Seth",
  "picture": "https://...",
  "provider": "google",
  "providerId": "1234567890",
  "iat": 1714000000,
  "exp": 1714086400
}
```

Client apps can decode this with any JWT library to get user info without calling back to the auth API.

---

## Security Notes

- **Whitelist redirect URIs** in `app.allowed-redirect-uris` — only your known client app domains
- **Change the JWT secret** to a strong random value in production
- **Use HTTPS** in production for all redirect URIs
- The JWT is signed with HMAC-SHA256 — client apps can verify it using your public secret (or keep verification server-side via `/auth/verify`)
