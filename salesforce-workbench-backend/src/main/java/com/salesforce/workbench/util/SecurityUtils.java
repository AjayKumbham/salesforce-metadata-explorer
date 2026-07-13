package com.salesforce.workbench.util;

import com.salesforce.workbench.exception.InvalidConnectionException;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static String extractConnectionId(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String connectionId = authHeader.substring(7).trim();
            if (!connectionId.isEmpty()) {
                return connectionId;
            }
        }
        throw new InvalidConnectionException("Missing or invalid Authorization header");
    }
}
