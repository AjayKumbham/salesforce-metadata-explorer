package com.salesforce.workbench.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "salesforce_connections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesforceConnection {

    @Id
    @Column(name = "connection_id", nullable = false, updatable = false)
    private String connectionId;

    @Column(name = "encrypted_access_token", nullable = false, length = 1024)
    private String accessToken;

    @Column(name = "server_url", nullable = false)
    private String serverUrl;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "auth_method", nullable = false, length = 20)
    private String authMethod;

    @Column(name = "encrypted_refresh_token", length = 2048)
    private String refreshToken;

    @Column(name = "encrypted_password", length = 2048)
    private String password;

    @Column(name = "encrypted_security_token", length = 2048)
    private String securityToken;

    @Column(name = "environment", length = 20)
    private String environment;

    @Column(name = "api_version", length = 10)
    private String apiVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_accessed_at", nullable = false)
    private LocalDateTime lastAccessedAt;
}
