package com.salesforce.workbench.service.impl;

import com.salesforce.workbench.config.SalesforceProperties;
import com.salesforce.workbench.entity.SalesforceConnection;
import com.salesforce.workbench.repository.ConnectionRepository;
import com.salesforce.workbench.service.ConnectionService;
import com.salesforce.workbench.util.CryptoUtil;
import com.salesforce.workbench.util.SalesforceUrlUtil;
import com.sforce.soap.partner.GetUserInfoResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.salesforce.workbench.exception.ResourceNotFoundException;
import com.sforce.ws.ConnectorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ConnectionServiceImpl implements ConnectionService {

    private static final String AUTH_METHOD_OAUTH = "OAUTH";
    private static final String AUTH_METHOD_USERNAME_PASSWORD = "USERNAME_PASSWORD";
    private static final String ENV_SANDBOX = "Sandbox";

    private final ConnectionRepository connectionRepository;
    private final SalesforceProperties properties;
    private final RestTemplate restTemplate;

    public ConnectionServiceImpl(ConnectionRepository connectionRepository, SalesforceProperties properties, RestTemplate restTemplate) {
        this.connectionRepository = connectionRepository;
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public SalesforceConnection createOAuthConnection(PartnerConnection connection, String refreshToken, String environment, String apiVersion) throws ConnectionException {
        String encryptedRefreshToken = CryptoUtil.encrypt(refreshToken, properties.getEncryptionKey());
        return saveConnection(connection, AUTH_METHOD_OAUTH, encryptedRefreshToken, null, null, environment, apiVersion);
    }

    @Override
    public SalesforceConnection createUsernamePasswordConnection(PartnerConnection connection, String password, String securityToken, String environment, String apiVersion) throws ConnectionException {
        String encryptedPassword = CryptoUtil.encrypt(password, properties.getEncryptionKey());
        String encryptedToken = securityToken != null ? CryptoUtil.encrypt(securityToken, properties.getEncryptionKey()) : null;
        return saveConnection(connection, AUTH_METHOD_USERNAME_PASSWORD, null, encryptedPassword, encryptedToken, environment, apiVersion);
    }

    @Override
    @Transactional
    public PartnerConnection getPartnerConnection(String connectionId) throws ConnectionException {
        SalesforceConnection sfConnection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ConnectionException("Invalid or expired session"));
        sfConnection.setLastAccessedAt(LocalDateTime.now());
        connectionRepository.save(sfConnection);

        ConnectorConfig config = new ConnectorConfig();
        config.setSessionId(CryptoUtil.decrypt(sfConnection.getAccessToken(), properties.getEncryptionKey()));
        config.setServiceEndpoint(sfConnection.getServerUrl());
        return new PartnerConnection(config);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesforceConnection getConnectionEntity(String connectionId) {
        return connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));
    }

    @Override
    @Transactional
    public void removeConnection(String connectionId) {
        connectionRepository.deleteById(connectionId);
        log.debug("Removed connection");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateConnection(String connectionId) {
        return connectionRepository.existsById(connectionId);
    }

    @Override
    @Transactional
    public PartnerConnection refreshConnection(String connectionId) throws ConnectionException {
        SalesforceConnection sfConnection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ConnectionException("Invalid or expired session"));

        log.debug("Attempting to auto-refresh connection");

        if (AUTH_METHOD_OAUTH.equals(sfConnection.getAuthMethod()) && sfConnection.getRefreshToken() != null) {
            String decryptedRefreshToken = CryptoUtil.decrypt(sfConnection.getRefreshToken(), properties.getEncryptionKey());
            return refreshOAuthConnection(sfConnection, decryptedRefreshToken);
        } else if (AUTH_METHOD_USERNAME_PASSWORD.equals(sfConnection.getAuthMethod()) && sfConnection.getPassword() != null) {
            return refreshUsernamePasswordConnection(sfConnection);
        }

        throw new ConnectionException("Cannot refresh connection, missing refresh artifacts");
    }

    private SalesforceConnection saveConnection(PartnerConnection connection, String authMethod, String refreshToken, String encryptedPassword, String encryptedToken, String environment, String apiVersion) throws ConnectionException {
        GetUserInfoResult userInfo = connection.getUserInfo();
        String connectionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        
        SalesforceConnection sfConnection = SalesforceConnection.builder()
                .connectionId(connectionId)
                .accessToken(CryptoUtil.encrypt(connection.getConfig().getSessionId(), properties.getEncryptionKey()))
                .serverUrl(connection.getConfig().getServiceEndpoint())
                .username(userInfo.getUserName())
                .userId(userInfo.getUserId())
                .organizationId(userInfo.getOrganizationId())
                .authMethod(authMethod)
                .refreshToken(refreshToken)
                .password(encryptedPassword)
                .securityToken(encryptedToken)
                .environment(environment)
                .apiVersion(apiVersion)
                .createdAt(now)
                .lastAccessedAt(now)
                .build();

        connectionRepository.save(sfConnection);
        log.debug("Created new connection");
        return sfConnection;
    }

    @Transactional
    private PartnerConnection refreshOAuthConnection(SalesforceConnection sfConnection, String decryptedRefreshToken) throws ConnectionException {
        try {
            String tokenUrl = SalesforceUrlUtil.extractBaseUrl(properties.getLoginUrl()) + "/services/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", "refresh_token");
            map.add("client_id", properties.getOauth().getClientId());
            map.add("client_secret", properties.getOauth().getClientSecret());
            map.add("refresh_token", decryptedRefreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<OAuthTokenResponse> response = restTemplate.postForEntity(tokenUrl, request, OAuthTokenResponse.class);
            OAuthTokenResponse tokenResponse = response.getBody();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new ConnectionException("Failed to refresh OAuth token.");
            }

            sfConnection.setAccessToken(CryptoUtil.encrypt(tokenResponse.getAccessToken(), properties.getEncryptionKey()));
            sfConnection.setServerUrl(tokenResponse.getInstanceUrl() + "/services/Soap/u/" + sfConnection.getApiVersion());
            sfConnection.setLastAccessedAt(LocalDateTime.now());
            connectionRepository.save(sfConnection);

            ConnectorConfig config = new ConnectorConfig();
            config.setSessionId(tokenResponse.getAccessToken());
            config.setServiceEndpoint(sfConnection.getServerUrl());
            return new PartnerConnection(config);
        } catch (Exception e) {
            connectionRepository.delete(sfConnection);
            throw new ConnectionException("Failed to auto-refresh OAuth connection", e);
        }
    }

    @Transactional
    private PartnerConnection refreshUsernamePasswordConnection(SalesforceConnection sfConnection) throws ConnectionException {
        try {
            String decryptedPassword = CryptoUtil.decrypt(sfConnection.getPassword(), properties.getEncryptionKey());
            String baseLoginUrl = ENV_SANDBOX.equalsIgnoreCase(sfConnection.getEnvironment()) ? properties.getSandboxUrl() : properties.getLoginUrl();
            String loginUrl = baseLoginUrl + "/services/Soap/u/" + sfConnection.getApiVersion();

            ConnectorConfig config = new ConnectorConfig();
            String decryptedSecurityToken = sfConnection.getSecurityToken() != null
                    ? CryptoUtil.decrypt(sfConnection.getSecurityToken(), properties.getEncryptionKey())
                    : "";
            config.setUsername(sfConnection.getUsername());
            config.setPassword(decryptedPassword + decryptedSecurityToken);
            config.setAuthEndpoint(loginUrl);

            PartnerConnection connection = new PartnerConnection(config);

            sfConnection.setAccessToken(CryptoUtil.encrypt(connection.getConfig().getSessionId(), properties.getEncryptionKey()));
            sfConnection.setServerUrl(connection.getConfig().getServiceEndpoint());
            sfConnection.setLastAccessedAt(LocalDateTime.now());
            connectionRepository.save(sfConnection);

            return connection;
        } catch (Exception e) {
            connectionRepository.delete(sfConnection);
            throw new ConnectionException("Failed to auto-refresh username/password connection", e);
        }
    }

    @Getter
    private static class OAuthTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("instance_url")
        private String instanceUrl;
    }
}
