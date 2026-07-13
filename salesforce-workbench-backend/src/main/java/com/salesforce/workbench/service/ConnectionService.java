package com.salesforce.workbench.service;

import com.salesforce.workbench.entity.SalesforceConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

public interface ConnectionService {

    SalesforceConnection createOAuthConnection(PartnerConnection partnerConnection, String refreshToken, String environment, String apiVersion) throws ConnectionException;

    SalesforceConnection createUsernamePasswordConnection(PartnerConnection partnerConnection, String password, String securityToken, String environment, String apiVersion) throws ConnectionException;

    PartnerConnection getPartnerConnection(String connectionId) throws ConnectionException;

    SalesforceConnection getConnectionEntity(String connectionId);

    void removeConnection(String connectionId);

    boolean validateConnection(String connectionId);

    PartnerConnection refreshConnection(String connectionId) throws ConnectionException;
}
