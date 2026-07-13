package com.salesforce.workbench.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.salesforce.workbench.util.SalesforceUrlUtil;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Getter
@Slf4j
public class OAuthAuthenticator implements SalesforceAuthenticator {

    private final String authCode;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String loginUrl;
    private final String codeVerifier;
    private final RestTemplate restTemplate;
    private final String apiVersion;

    private String refreshToken;

    public OAuthAuthenticator(String authCode, String clientId, String clientSecret, String redirectUri, String loginUrl, String codeVerifier, RestTemplate restTemplate, String apiVersion) {
        this.authCode = authCode;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.codeVerifier = codeVerifier;
        this.loginUrl = SalesforceUrlUtil.extractBaseUrl(loginUrl);
        this.restTemplate = restTemplate;
        this.apiVersion = apiVersion;
    }

    @Override
    public PartnerConnection authenticate() throws ConnectionException {
        try {
            String tokenUrl = this.loginUrl + "/services/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", "authorization_code");
            map.add("client_id", clientId);
            map.add("client_secret", clientSecret);
            map.add("redirect_uri", redirectUri);
            map.add("code", authCode);
            if (codeVerifier != null && !codeVerifier.isEmpty()) {
                map.add("code_verifier", codeVerifier);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            ResponseEntity<OAuthTokenResponse> response = restTemplate.postForEntity(tokenUrl, request, OAuthTokenResponse.class);
            OAuthTokenResponse tokenResponse = response.getBody();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new ConnectionException("Failed to obtain access token from OAuth endpoint.");
            }

            this.refreshToken = tokenResponse.getRefreshToken();

            ConnectorConfig config = new ConnectorConfig();
            config.setSessionId(tokenResponse.getAccessToken());
            config.setServiceEndpoint(tokenResponse.getInstanceUrl() + "/services/Soap/u/" + this.apiVersion);

            log.info("Successfully obtained OAuth token for instance: {}", tokenResponse.getInstanceUrl());
            return new PartnerConnection(config);

        } catch (RestClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("OAuth authentication HTTP error: {} - {}", e.getStatusCode(), responseBody);
            throw new ConnectionException("OAuth Authentication failed: " + responseBody, e);
        } catch (Exception e) {
            log.error("OAuth authentication failed", e);
            throw new ConnectionException("OAuth Authentication failed: " + e.getMessage(), e);
        }
    }

    @Getter
    private static final class OAuthTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("instance_url")
        private String instanceUrl;
        @JsonProperty("refresh_token")
        private String refreshToken;
    }
}
