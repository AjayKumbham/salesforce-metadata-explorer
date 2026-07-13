package com.salesforce.workbench.service.impl;

import com.salesforce.workbench.config.SalesforceProperties;
import com.salesforce.workbench.dto.MetadataComponentInfo;
import com.salesforce.workbench.dto.MetadataTypeInfo;
import com.salesforce.workbench.entity.MetadataComponent;
import com.salesforce.workbench.entity.MetadataType;
import com.salesforce.workbench.entity.SalesforceConnection;
import com.salesforce.workbench.exception.InvalidConnectionException;
import com.salesforce.workbench.exception.MetadataServiceException;
import com.salesforce.workbench.repository.ConnectionRepository;
import com.salesforce.workbench.repository.MetadataComponentRepository;
import com.salesforce.workbench.repository.MetadataTypeRepository;
import com.salesforce.workbench.service.ConnectionService;
import com.salesforce.workbench.service.MetadataService;
import com.salesforce.workbench.util.CryptoUtil;
import com.sforce.soap.metadata.DescribeMetadataObject;
import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class MetadataServiceImpl implements MetadataService {

    private static final int MAX_DB_STRING_LENGTH = 1000;
    private static final String PARTNER_SOAP_PATH = "/Soap/u/";
    private static final String METADATA_SOAP_PATH = "/Soap/m/";

    private final ConnectionRepository connectionRepository;
    private final MetadataTypeRepository typeRepository;
    private final MetadataComponentRepository componentRepository;
    private final SalesforceProperties properties;
    private final ConnectionService connectionService;

    public MetadataServiceImpl(
            ConnectionRepository connectionRepository,
            MetadataTypeRepository typeRepository,
            MetadataComponentRepository componentRepository,
            SalesforceProperties properties,
            ConnectionService connectionService) {
        this.connectionRepository = connectionRepository;
        this.typeRepository = typeRepository;
        this.componentRepository = componentRepository;
        this.properties = properties;
        this.connectionService = connectionService;
    }

    @Override
    public List<MetadataTypeInfo> describeMetadataTypes(String connectionId) {
        List<MetadataType> persistedTypes = typeRepository.findByConnectionIdOrderByXmlNameAsc(connectionId);
        if (!persistedTypes.isEmpty()) {
            log.debug("Returning metadata types from database");
            return persistedTypes.stream().map(this::toDto).toList();
        }

        try {
            return fetchAndSaveMetadataTypes(connectionId);
        } catch (ConnectionException e) {
            if (e.getMessage() != null && e.getMessage().contains("INVALID_SESSION_ID")) {
                try {
                    log.info("Session expired for metadata types, attempting to refresh...");
                    connectionService.refreshConnection(connectionId);
                    return fetchAndSaveMetadataTypes(connectionId);
                } catch (ConnectionException refreshEx) {
                    throw new InvalidConnectionException("Session expired and could not be refreshed.");
                } catch (Exception ex) {
                    throw new MetadataServiceException("Failed to describe metadata types after refresh: " + ex.getMessage(), ex);
                }
            }
            throw new MetadataServiceException("Failed to describe metadata types: " + e.getMessage(), e);
        } catch (InvalidConnectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to describe metadata types", e);
            throw new MetadataServiceException("Failed to describe metadata types: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<MetadataComponentInfo> listComponents(String connectionId, String type, String folder, String search, int page, int size) {
        String safeSearch = search == null ? "" : search;
        Pageable pageable = PageRequest.of(page, size);
        
        boolean isCached = componentRepository.existsByConnectionIdAndType(connectionId, type);
        
        if (isCached) {
            log.debug("Returning components of type {} from database", type);
            return componentRepository.findByConnectionIdAndTypeAndFullNameContainingIgnoreCaseOrderByFullNameAsc(
                    connectionId, type, safeSearch, pageable).map(this::toDto);
        }

        try {
            fetchAndSaveComponents(connectionId, type, folder);
            return componentRepository.findByConnectionIdAndTypeAndFullNameContainingIgnoreCaseOrderByFullNameAsc(
                    connectionId, type, safeSearch, pageable).map(this::toDto);
        } catch (ConnectionException e) {
            if (e.getMessage() != null && e.getMessage().contains("INVALID_SESSION_ID")) {
                try {
                    log.info("Session expired for components, attempting to refresh...");
                    connectionService.refreshConnection(connectionId);
                    fetchAndSaveComponents(connectionId, type, folder);
                    return componentRepository.findByConnectionIdAndTypeAndFullNameContainingIgnoreCaseOrderByFullNameAsc(
                            connectionId, type, safeSearch, pageable).map(this::toDto);
                } catch (ConnectionException refreshEx) {
                    throw new InvalidConnectionException("Session expired and could not be refreshed.");
                } catch (Exception ex) {
                    throw new MetadataServiceException("Failed to list components after refresh: " + ex.getMessage(), ex);
                }
            }
            throw new MetadataServiceException("Failed to list components: " + e.getMessage(), e);
        } catch (InvalidConnectionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to list components of type {}", type, e);
            throw new MetadataServiceException("Failed to list components: " + e.getMessage(), e);
        }
    }

    private List<MetadataTypeInfo> fetchAndSaveMetadataTypes(String connectionId) throws Exception {
        log.debug("Fetching metadata types from Salesforce API");
        SalesforceConnection sfConnection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new InvalidConnectionException("Invalid or expired connection"));
        MetadataConnection metadataConnection = buildMetadataConnection(sfConnection);
        
        double apiVersion = parseApiVersion(sfConnection);
        DescribeMetadataResult result = metadataConnection.describeMetadata(apiVersion);

        List<MetadataTypeInfo> types = Arrays.stream(result.getMetadataObjects())
                .map(this::toMetadataTypeInfo)
                .sorted((a, b) -> a.getXmlName().compareToIgnoreCase(b.getXmlName()))
                .toList();
        
        saveTypesToDb(connectionId, types);
        return types;
    }

    private void fetchAndSaveComponents(String connectionId, String type, String folder) throws Exception {
        log.debug("Fetching components of type {} from Salesforce API", type);
        SalesforceConnection sfConnection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new InvalidConnectionException("Invalid or expired connection"));
        MetadataConnection metadataConnection = buildMetadataConnection(sfConnection);
        
        double apiVersion = parseApiVersion(sfConnection);

        ListMetadataQuery query = new ListMetadataQuery();
        query.setType(type);
        if (folder != null && !folder.isBlank()) {
            query.setFolder(folder);
        }

        FileProperties[] components = metadataConnection.listMetadata(new ListMetadataQuery[]{query}, apiVersion);

        List<MetadataComponentInfo> resultList = components == null ? Collections.emptyList() :
                Arrays.stream(components)
                        .map(this::toMetadataComponentInfo)
                        .sorted((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()))
                        .toList();

        saveComponentsToDb(connectionId, type, resultList);
    }

    @Override
    @Transactional
    public void clearCache(String connectionId) {
        log.debug("Evicting metadata cache");
        typeRepository.deleteByConnectionId(connectionId);
        componentRepository.deleteByConnectionId(connectionId);
    }

    private void saveTypesToDb(String connectionId, List<MetadataTypeInfo> dtos) {
        List<MetadataType> entities = dtos.stream().map(dto -> MetadataType.builder()
                .id(UUID.randomUUID().toString())
                .connectionId(connectionId)
                .xmlName(dto.getXmlName())
                .directoryName(dto.getDirectoryName())
                .inFolder(dto.isInFolder())
                .metaFile(dto.isMetaFile())
                .suffix(dto.getSuffix())
                .build()).toList();
        typeRepository.saveAll(entities);
    }

    private void saveComponentsToDb(String connectionId, String type, List<MetadataComponentInfo> dtos) {
        List<MetadataComponent> entities = dtos.stream().map(dto -> MetadataComponent.builder()
                .id(UUID.randomUUID().toString())
                .connectionId(connectionId)
                .type(type)
                .fullName(truncate(dto.getFullName(), MAX_DB_STRING_LENGTH))
                .fileName(truncate(dto.getFileName(), MAX_DB_STRING_LENGTH))
                .namespacePrefix(dto.getNamespacePrefix())
                .salesforceId(dto.getId())
                .lastModifiedDate(dto.getLastModifiedDate())
                .createdDate(dto.getCreatedDate())
                .lastModifiedByName(dto.getLastModifiedByName())
                .createdByName(dto.getCreatedByName())
                .build()).toList();
        componentRepository.saveAll(entities);
    }

    private MetadataTypeInfo toDto(MetadataType entity) {
        return MetadataTypeInfo.builder()
                .xmlName(entity.getXmlName())
                .directoryName(entity.getDirectoryName())
                .inFolder(entity.isInFolder())
                .metaFile(entity.isMetaFile())
                .suffix(entity.getSuffix())
                .childXmlNames(Collections.emptyList())
                .build();
    }

    private MetadataComponentInfo toDto(MetadataComponent entity) {
        return MetadataComponentInfo.builder()
                .fullName(entity.getFullName())
                .type(entity.getType())
                .fileName(entity.getFileName())
                .namespacePrefix(entity.getNamespacePrefix())
                .id(entity.getSalesforceId())
                .lastModifiedDate(entity.getLastModifiedDate())
                .createdDate(entity.getCreatedDate())
                .lastModifiedByName(entity.getLastModifiedByName())
                .createdByName(entity.getCreatedByName())
                .build();
    }

    private MetadataConnection buildMetadataConnection(SalesforceConnection sfConnection) throws Exception {
        String metadataUrl = deriveMetadataUrl(sfConnection.getServerUrl());
        String sessionId = CryptoUtil.decrypt(
                sfConnection.getAccessToken(), properties.getEncryptionKey());

        ConnectorConfig config = new ConnectorConfig();
        config.setSessionId(sessionId);
        config.setServiceEndpoint(metadataUrl);

        log.debug("Building MetadataConnection for user {} at {}", sfConnection.getUsername(), metadataUrl);
        return new MetadataConnection(config);
    }

    private String deriveMetadataUrl(String partnerUrl) {
        return partnerUrl.replace(PARTNER_SOAP_PATH, METADATA_SOAP_PATH);
    }

    private MetadataTypeInfo toMetadataTypeInfo(DescribeMetadataObject obj) {
        List<String> childNames = obj.getChildXmlNames() != null
                ? Arrays.asList(obj.getChildXmlNames())
                : Collections.emptyList();

        return MetadataTypeInfo.builder()
                .xmlName(obj.getXmlName())
                .directoryName(obj.getDirectoryName())
                .inFolder(obj.isInFolder())
                .metaFile(obj.getMetaFile())
                .suffix(obj.getSuffix())
                .childXmlNames(childNames)
                .build();
    }

    private MetadataComponentInfo toMetadataComponentInfo(FileProperties fp) {
        return MetadataComponentInfo.builder()
                .fullName(fp.getFullName())
                .type(fp.getType())
                .fileName(fp.getFileName())
                .namespacePrefix(fp.getNamespacePrefix())
                .id(fp.getId())
                .lastModifiedDate(toLocalDateTime(fp.getLastModifiedDate()))
                .createdDate(toLocalDateTime(fp.getCreatedDate()))
                .lastModifiedByName(fp.getLastModifiedByName())
                .createdByName(fp.getCreatedByName())
                .build();
    }

    private LocalDateTime toLocalDateTime(Calendar calendar) {
        if (calendar == null || calendar.getTimeInMillis() == 0) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(calendar.getTimeInMillis()),
                ZoneId.systemDefault());
    }

    private double parseApiVersion(SalesforceConnection connection) {
        if (connection.getApiVersion() == null) {
            throw new IllegalStateException("API Version is not set for this connection");
        }
        return Double.parseDouble(connection.getApiVersion());
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
