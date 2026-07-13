package com.salesforce.workbench.util;

public class SalesforceUrlUtil {

    private SalesforceUrlUtil() {
    }

    public static String extractBaseUrl(String url) {
        if (url != null && url.contains("/services/Soap")) {
            return url.substring(0, url.indexOf("/services/Soap"));
        }
        return url;
    }
}
