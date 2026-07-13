package com.salesforce.workbench.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MetadataComponentInfo {
    private String fullName;
    private String type;
    private String fileName;
    private String namespacePrefix;
    private String id;
    private LocalDateTime lastModifiedDate;
    private LocalDateTime createdDate;
    private String lastModifiedByName;
    private String createdByName;
}
