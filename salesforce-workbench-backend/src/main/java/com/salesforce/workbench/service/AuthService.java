package com.salesforce.workbench.service;

import com.salesforce.workbench.dto.AuthResponse;
import com.salesforce.workbench.dto.UsernamePasswordLoginRequest;

public interface AuthService {
    AuthResponse usernamePasswordLogin(UsernamePasswordLoginRequest loginRequest);
    String getAuthorizationUri(String environment, String codeChallenge, String state);
    AuthResponse oauthLogin(String code, String environment, String codeVerifier, String apiVersion);
    void logout(String connectionId);
    AuthResponse getConnectionInfo(String connectionId);
}
