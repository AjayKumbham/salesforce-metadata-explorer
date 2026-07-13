package com.salesforce.workbench.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MetadataTypeInfo {
    private String xmlName;
    private String directoryName;
    private boolean inFolder;
    private boolean metaFile;
    private String suffix;
    private List<String> childXmlNames;
}
