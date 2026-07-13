package com.salesforce.workbench.auth;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import lombok.Getter;

@Getter
public class UsernamePasswordAuthenticator implements SalesforceAuthenticator {

    private final String username;
    private final String password;
    private final String securityToken;
    private final String loginUrl;

    public UsernamePasswordAuthenticator(String username, String password, String securityToken, String loginUrl) {
        this.username = username;
        this.password = password;
        this.securityToken = securityToken != null ? securityToken : "";
        this.loginUrl = loginUrl;
    }

    @Override
    public PartnerConnection authenticate() throws ConnectionException {
        ConnectorConfig config = new ConnectorConfig();
        config.setUsername(username);
        config.setPassword(password + securityToken);
        config.setAuthEndpoint(loginUrl);
        return new PartnerConnection(config);
    }
}
