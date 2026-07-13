package com.salesforce.workbench.controller;

import com.salesforce.workbench.dto.MetadataComponentInfo;
import com.salesforce.workbench.dto.MetadataTypeInfo;
import com.salesforce.workbench.service.MetadataService;
import com.salesforce.workbench.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.data.domain.Page;
import java.util.List;

@RestController
@RequestMapping("/api/metadata")
@Slf4j
@Validated
public class MetadataController {

    private static final String DEFAULT_PAGE_NUMBER = "0";
    private static final String DEFAULT_PAGE_SIZE = "50";

    private final MetadataService metadataService;

    public MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/types")
    public ResponseEntity<List<MetadataTypeInfo>> describeTypes(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader) {
        String connectionId = SecurityUtils.extractConnectionId(authHeader);
        log.info("Describing metadata types");
        List<MetadataTypeInfo> types = metadataService.describeMetadataTypes(connectionId);
        return ResponseEntity.ok(types);
    }

    @GetMapping("/components")
    public ResponseEntity<Page<MetadataComponentInfo>> listComponents(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestParam @NotBlank(message = "Type is required") String type,
            @RequestParam(required = false) String folder,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(200) int size) {
        String connectionId = SecurityUtils.extractConnectionId(authHeader);
        log.info("Listing components of type '{}'", type);
        Page<MetadataComponentInfo> components = metadataService.listComponents(connectionId, type, folder, search, page, size);
        return ResponseEntity.ok(components);
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearCache(@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader) {
        String connectionId = SecurityUtils.extractConnectionId(authHeader);
        log.info("Processing cache eviction");
        metadataService.clearCache(connectionId);
        return ResponseEntity.noContent().build();
    }
}
