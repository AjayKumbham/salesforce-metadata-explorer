package com.salesforce.workbench.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UsernamePasswordLoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private String securityToken;

    @NotBlank(message = "Environment is required")
    private String environment;

    @NotBlank(message = "API version is required")
    private String apiVersion;
}
