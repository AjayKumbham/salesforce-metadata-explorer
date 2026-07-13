package com.salesforce.workbench;

import com.salesforce.workbench.config.SalesforceProperties;
import com.salesforce.workbench.entity.SalesforceConnection;
import com.salesforce.workbench.exception.ResourceNotFoundException;
import com.salesforce.workbench.repository.ConnectionRepository;
import com.salesforce.workbench.service.impl.ConnectionServiceImpl;
import com.salesforce.workbench.util.CryptoUtil;
import com.sforce.soap.partner.GetUserInfoResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceImplTest {

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private SalesforceProperties properties;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ConnectionServiceImpl connectionService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getConnectionEntity_whenExists_returnsEntity() {
        SalesforceConnection mockConn = new SalesforceConnection();
        mockConn.setConnectionId("test-id");
        when(connectionRepository.findById("test-id")).thenReturn(Optional.of(mockConn));

        SalesforceConnection result = connectionService.getConnectionEntity("test-id");

        assertNotNull(result);
        assertEquals("test-id", result.getConnectionId());
    }

    @Test
    void getConnectionEntity_whenNotExists_throwsException() {
        when(connectionRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            connectionService.getConnectionEntity("invalid-id");
        });
    }

    @Test
    void validateConnection_whenExists_returnsTrue() {
        when(connectionRepository.existsById("test-id")).thenReturn(true);
        assertTrue(connectionService.validateConnection("test-id"));
    }

    @Test
    void validateConnection_whenNotExists_returnsFalse() {
        when(connectionRepository.existsById("invalid-id")).thenReturn(false);
        assertFalse(connectionService.validateConnection("invalid-id"));
    }

    @Test
    void removeConnection_deletesById() {
        connectionService.removeConnection("test-id");
        verify(connectionRepository, times(1)).deleteById("test-id");
    }

    @Test
    void createOAuthConnection_savesConnection() throws ConnectionException {
        PartnerConnection partnerConn = mock(PartnerConnection.class);
        GetUserInfoResult userInfo = new GetUserInfoResult();
        userInfo.setUserId("user123");
        userInfo.setOrganizationId("org123");
        userInfo.setUserName("testuser@example.com");
        when(partnerConn.getUserInfo()).thenReturn(userInfo);
        
        when(partnerConn.getConfig()).thenReturn(new com.sforce.ws.ConnectorConfig());
        partnerConn.getConfig().setServiceEndpoint("https://test.salesforce.com/services/Soap/u/60.0");
        partnerConn.getConfig().setSessionId("test-session");

        when(properties.getEncryptionKey()).thenReturn("12345678901234567890123456789012");
        when(connectionRepository.save(any(SalesforceConnection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalesforceConnection savedConn = connectionService.createOAuthConnection(partnerConn, "dummy-refresh-token", "Sandbox", "60.0");

        assertNotNull(savedConn);
        assertEquals("user123", savedConn.getUserId());
        assertEquals("OAUTH", savedConn.getAuthMethod());
    }

    @Test
    void refreshConnection_withInvalidId_throwsException() {
        when(connectionRepository.findById("invalid-id")).thenReturn(Optional.empty());
        assertThrows(ConnectionException.class, () -> connectionService.refreshConnection("invalid-id"));
    }
}
