package com.salesforce.workbench.service.impl;

import com.salesforce.workbench.auth.OAuthAuthenticator;
import com.salesforce.workbench.auth.SalesforceAuthenticator;
import com.salesforce.workbench.auth.UsernamePasswordAuthenticator;
import com.salesforce.workbench.config.SalesforceProperties;
import com.salesforce.workbench.dto.AuthResponse;
import com.salesforce.workbench.dto.UsernamePasswordLoginRequest;
import com.salesforce.workbench.dto.LoginStatus;
import com.salesforce.workbench.entity.SalesforceConnection;
import com.salesforce.workbench.exception.AuthenticationFailedException;
import com.salesforce.workbench.exception.InvalidConnectionException;
import com.salesforce.workbench.service.ConnectionService;
import com.salesforce.workbench.service.AuthService;
import com.salesforce.workbench.util.SalesforceUrlUtil;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String ENV_SANDBOX = "Sandbox";
    private static final String ENV_PRODUCTION = "Production";

    private final SalesforceProperties properties;
    private final ConnectionService connectionService;
    private final RestTemplate restTemplate;

    public AuthServiceImpl(SalesforceProperties properties, ConnectionService connectionService, RestTemplate restTemplate) {
        this.properties = properties;
        this.connectionService = connectionService;
        this.restTemplate = restTemplate;
    }

    @Override
    public AuthResponse usernamePasswordLogin(UsernamePasswordLoginRequest loginRequest) {
        String baseLoginUrl = getBaseLoginUrl(loginRequest.getEnvironment());
        String fullLoginUrl = baseLoginUrl + "/services/Soap/u/" + loginRequest.getApiVersion();

        SalesforceAuthenticator authenticator = new UsernamePasswordAuthenticator(
                loginRequest.getUsername(),
                loginRequest.getPassword(),
                loginRequest.getSecurityToken(),
                fullLoginUrl
        );

        return authenticateAndCreateConnection(authenticator, loginRequest.getEnvironment(), loginRequest.getApiVersion());
    }


    @Override
    public String getAuthorizationUri(String environment, String codeChallenge, String state) {
        String loginUrl = SalesforceUrlUtil.extractBaseUrl(getBaseLoginUrl(environment));

        return UriComponentsBuilder.fromHttpUrl(loginUrl)
                .path("/services/oauth2/authorize")
                .queryParam("client_id", properties.getOauth().getClientId())
                .queryParam("redirect_uri", properties.getOauth().getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("prompt", "login consent")
                .queryParam("scope", "refresh_token offline_access api web")
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public AuthResponse oauthLogin(String code, String environment, String codeVerifier, String apiVersion) {
        String loginUrl = getBaseLoginUrl(environment);

        OAuthAuthenticator authenticator = new OAuthAuthenticator(
                code,
                properties.getOauth().getClientId(),
                properties.getOauth().getClientSecret(),
                properties.getOauth().getRedirectUri(),
                loginUrl,
                codeVerifier,
                restTemplate,
                apiVersion
        );

        return authenticateAndCreateConnection(authenticator, environment, apiVersion);
    }

    @Override
    public void logout(String connectionId) {
        if (!connectionService.validateConnection(connectionId)) {
            throw new InvalidConnectionException("Session not found or already invalidated");
        }

        try {
            PartnerConnection connection = connectionService.getPartnerConnection(connectionId);
            connection.logout();
        } catch (ConnectionException e) {
            // Ignore Salesforce logout errors, just remove our local connection
        } finally {
            connectionService.removeConnection(connectionId);
        }
    }

    @Override
    public AuthResponse getConnectionInfo(String connectionId) {
        if (!connectionService.validateConnection(connectionId)) {
            throw new InvalidConnectionException("Invalid session");
        }

        try {
            PartnerConnection connection = connectionService.getPartnerConnection(connectionId);
            // Lightweight call to verify the session is still active on Salesforce side
            connection.getServerTimestamp();
            return buildResponseFromDb(connectionId);
        } catch (ConnectionException e) {
            try {
                connectionService.refreshConnection(connectionId);
                return buildResponseFromDb(connectionId);
            } catch (ConnectionException refreshEx) {
                connectionService.removeConnection(connectionId);
                throw new InvalidConnectionException("Session expired and could not be refreshed.");
            }
        }
    }

    private AuthResponse buildResponseFromDb(String connectionId) {
        SalesforceConnection sfConnection = connectionService.getConnectionEntity(connectionId);
        return buildAuthResponse(sfConnection, LoginStatus.ACTIVE);
    }

    private AuthResponse authenticateAndCreateConnection(SalesforceAuthenticator authenticator, String environment, String apiVersion) {
        try {
            PartnerConnection connection = authenticator.authenticate();
            SalesforceConnection sfConnection;

            if (authenticator instanceof OAuthAuthenticator oauth) {
                sfConnection = connectionService.createOAuthConnection(connection, oauth.getRefreshToken(), environment, apiVersion);
            } else if (authenticator instanceof UsernamePasswordAuthenticator upAuth) {
                String env = upAuth.getLoginUrl().contains("test.salesforce.com") ? ENV_SANDBOX : ENV_PRODUCTION;
                sfConnection = connectionService.createUsernamePasswordConnection(connection, upAuth.getPassword(), upAuth.getSecurityToken(), env, apiVersion);
            } else {
                throw new IllegalStateException("Unknown authenticator type");
            }

            return buildAuthResponse(sfConnection, LoginStatus.SUCCESS);
        } catch (ConnectionException e) {
            String msg = e.getMessage() != null ? e.getMessage() : (e.getCause() != null ? e.getCause().getMessage() : e.toString());
            throw new AuthenticationFailedException("Authentication failed: " + msg, e);
        }
    }
    private String getBaseLoginUrl(String environment) {
        return ENV_SANDBOX.equalsIgnoreCase(environment) ? properties.getSandboxUrl() : properties.getLoginUrl();
    }

    private AuthResponse buildAuthResponse(SalesforceConnection sfConnection, LoginStatus status) {
        return AuthResponse.builder()
                .connectionId(sfConnection.getConnectionId())
                .instanceUrl(sfConnection.getServerUrl())
                .username(sfConnection.getUsername())
                .userId(sfConnection.getUserId())
                .organizationId(sfConnection.getOrganizationId())
                .apiVersion(sfConnection.getApiVersion())
                .environment(sfConnection.getEnvironment())
                .loginStatus(status)
                .build();
    }
}
