package com.salesforce.workbench.service;

import com.salesforce.workbench.dto.MetadataComponentInfo;
import com.salesforce.workbench.dto.MetadataTypeInfo;

import org.springframework.data.domain.Page;
import java.util.List;

public interface MetadataService {

    List<MetadataTypeInfo> describeMetadataTypes(String connectionId);

    Page<MetadataComponentInfo> listComponents(String connectionId, String type, String folder, String search, int page, int size);

    void clearCache(String connectionId);
}
