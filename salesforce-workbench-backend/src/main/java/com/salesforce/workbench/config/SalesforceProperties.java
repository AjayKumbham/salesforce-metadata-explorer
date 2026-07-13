package com.salesforce.workbench.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "salesforce.auth")
public class SalesforceProperties {
    private String loginUrl;
    private String sandboxUrl;
    private String frontendUrl = "http://localhost:4200";
    private String encryptionKey;
    private OAuth oauth = new OAuth();

    @Getter
    @Setter
    public static class OAuth {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
    }
}
