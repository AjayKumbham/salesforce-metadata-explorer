package com.salesforce.workbench.controller;

import com.salesforce.workbench.config.SalesforceProperties;
import com.salesforce.workbench.dto.AuthResponse;
import com.salesforce.workbench.dto.UsernamePasswordLoginRequest;
import com.salesforce.workbench.service.AuthService;
import com.salesforce.workbench.util.PKCEUtil;
import com.salesforce.workbench.util.SecurityUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private static final String PKCE_COOKIE_NAME = "pkce_verifier";
    private static final String COOKIE_PATH = "/";
    private static final int PKCE_COOKIE_MAX_AGE_SECONDS = 300;

    private final AuthService authService;
    private final SalesforceProperties properties;

    public AuthController(AuthService authService, SalesforceProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> usernamePasswordLogin(@Valid @RequestBody UsernamePasswordLoginRequest request) {
        log.info("Processing username/password login request");
        AuthResponse response = authService.usernamePasswordLogin(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/oauth/login")
    public ResponseEntity<Void> oauthLoginRedirect(
            @RequestParam String environment,
            @RequestParam String apiVersion,
            HttpServletResponse response) {
        log.info("Initiating OAuth login redirect for environment: {}, API Version: {}", environment, apiVersion);

        String codeVerifier = PKCEUtil.generateCodeVerifier();
        String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);

        Cookie pkceCookie = new Cookie(PKCE_COOKIE_NAME, codeVerifier);
        pkceCookie.setHttpOnly(true);
        pkceCookie.setPath(COOKIE_PATH);
        pkceCookie.setMaxAge(PKCE_COOKIE_MAX_AGE_SECONDS);
        response.addCookie(pkceCookie);

        String stateParam = environment + "|" + apiVersion;
        String authEndpoint = authService.getAuthorizationUri(environment, codeChallenge, stateParam);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authEndpoint))
                .build();
    }

    @GetMapping("/oauth/callback")
    public ResponseEntity<Void> oauthCallback(
            @RequestParam String code,
            @RequestParam String state,
            @CookieValue(name = PKCE_COOKIE_NAME, required = false) String codeVerifier,
            HttpServletResponse httpResponse) {
        log.info("Processing OAuth callback");

        if (codeVerifier != null) {
            Cookie clearCookie = new Cookie(PKCE_COOKIE_NAME, null);
            clearCookie.setMaxAge(0);
            clearCookie.setPath(COOKIE_PATH);
            httpResponse.addCookie(clearCookie);
        }
        
        String[] stateParts = state.split("\\|");
        if (stateParts.length != 2) {
            throw new IllegalArgumentException("Invalid state parameter received from Salesforce");
        }
        String environment = stateParts[0];
        String apiVersion = stateParts[1];

        AuthResponse response = authService.oauthLogin(code, environment, codeVerifier, apiVersion);

        String redirectUrl = properties.getFrontendUrl() + "/oauth-callback?connectionId=" + response.getConnectionId();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader) {
        String connectionId = SecurityUtils.extractConnectionId(authHeader);
        log.info("Processing logout");
        authService.logout(connectionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/session")
    public ResponseEntity<AuthResponse> getSession(@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader) {
        String connectionId = SecurityUtils.extractConnectionId(authHeader);
        return ResponseEntity.ok(authService.getConnectionInfo(connectionId));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "Salesforce Workbench Backend"));
    }
}
