package com.salesforce.workbench.auth;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

public interface SalesforceAuthenticator {
    PartnerConnection authenticate() throws ConnectionException;
}
