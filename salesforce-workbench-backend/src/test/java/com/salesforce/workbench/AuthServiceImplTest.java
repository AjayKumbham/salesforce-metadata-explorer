package com.salesforce.workbench;

import com.salesforce.workbench.config.SalesforceProperties;
import com.salesforce.workbench.dto.AuthResponse;
import com.salesforce.workbench.dto.UsernamePasswordLoginRequest;
import com.salesforce.workbench.dto.LoginStatus;
import com.salesforce.workbench.entity.SalesforceConnection;
import com.salesforce.workbench.exception.AuthenticationFailedException;
import com.salesforce.workbench.exception.InvalidConnectionException;
import com.salesforce.workbench.service.ConnectionService;
import com.salesforce.workbench.service.impl.AuthServiceImpl;
import com.sforce.soap.partner.GetUserInfoResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectorConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SalesforceProperties properties;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void getConnectionInfo_validConnection_returnsActiveResponse() throws Exception {
        String connectionId = "test-connection-id";

        when(connectionService.validateConnection(connectionId)).thenReturn(true);

        PartnerConnection mockConnection = mock(PartnerConnection.class);
        when(connectionService.getPartnerConnection(connectionId)).thenReturn(mockConnection);

        SalesforceConnection mockSfConnection = new SalesforceConnection();
        mockSfConnection.setConnectionId(connectionId);
        mockSfConnection.setUsername("testuser@example.com");
        when(connectionService.getConnectionEntity(connectionId)).thenReturn(mockSfConnection);

        AuthResponse response = authService.getConnectionInfo(connectionId);

        assertNotNull(response);
        assertEquals(connectionId, response.getConnectionId());
        assertEquals("testuser@example.com", response.getUsername());
        assertEquals(LoginStatus.ACTIVE, response.getLoginStatus());
    }

    @Test
    void getConnectionInfo_invalidConnection_throwsException() {
        String connectionId = "invalid-id";
        when(connectionService.validateConnection(connectionId)).thenReturn(false);

        assertThrows(InvalidConnectionException.class, () -> authService.getConnectionInfo(connectionId));
    }

    @Test
    void logout_validConnection_removesConnection() throws Exception {
        String connectionId = "test-connection-id";
        when(connectionService.validateConnection(connectionId)).thenReturn(true);
        PartnerConnection mockConnection = mock(PartnerConnection.class);
        when(connectionService.getPartnerConnection(connectionId)).thenReturn(mockConnection);

        authService.logout(connectionId);

        verify(mockConnection, times(1)).logout();
        verify(connectionService, times(1)).removeConnection(connectionId);
    }

    @Test
    void logout_invalidConnection_throwsException() {
        String connectionId = "invalid-id";
        when(connectionService.validateConnection(connectionId)).thenReturn(false);

        assertThrows(InvalidConnectionException.class, () -> authService.logout(connectionId));
    }

    @Test
    void getAuthorizationUri_productionEnvironment_returnsCorrectUri() {
        when(properties.getLoginUrl()).thenReturn("https://login.salesforce.com");
        when(properties.getOauth()).thenReturn(mockOauth("clientId123", "https://localhost:8080/auth/oauth/callback"));

        String uri = authService.getAuthorizationUri("Production", "challenge123", "Production|60.0");

        assertTrue(uri.contains("login.salesforce.com"));
        assertTrue(uri.contains("client_id=clientId123"));
        assertTrue(uri.contains("code_challenge=challenge123"));
        assertTrue(uri.contains("response_type=code"));
        assertTrue(uri.contains("state=Production%7C60.0") || uri.contains("state=Production|60.0"));
    }

    @Test
    void getAuthorizationUri_sandboxEnvironment_usesSandboxUrl() {
        when(properties.getSandboxUrl()).thenReturn("https://test.salesforce.com");
        when(properties.getOauth()).thenReturn(mockOauth("clientId123", "https://localhost:8080/auth/oauth/callback"));

        String uri = authService.getAuthorizationUri("Sandbox", "challenge456", "Sandbox|60.0");

        assertTrue(uri.contains("test.salesforce.com"));
    }

    private SalesforceProperties.OAuth mockOauth(String clientId, String redirectUri) {
        SalesforceProperties.OAuth oauth = new SalesforceProperties.OAuth();
        oauth.setClientId(clientId);
        oauth.setRedirectUri(redirectUri);
        return oauth;
    }
}
