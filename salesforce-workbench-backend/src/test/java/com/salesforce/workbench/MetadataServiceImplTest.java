package com.salesforce.workbench;

import com.salesforce.workbench.config.SalesforceProperties;
import com.salesforce.workbench.dto.MetadataTypeInfo;
import com.salesforce.workbench.entity.MetadataType;
import com.salesforce.workbench.repository.ConnectionRepository;
import com.salesforce.workbench.repository.MetadataComponentRepository;
import com.salesforce.workbench.repository.MetadataTypeRepository;
import com.salesforce.workbench.service.impl.MetadataServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetadataServiceImplTest {

    @Mock
    private MetadataTypeRepository typeRepository;

    @Mock
    private MetadataComponentRepository componentRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private SalesforceProperties properties;

    @InjectMocks
    private MetadataServiceImpl metadataService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void describeMetadataTypes_whenCacheExists_returnsFromCache() {
        String connectionId = "test-conn-id";
        MetadataType mockType = MetadataType.builder()
                .connectionId(connectionId)
                .xmlName("ApexClass")
                .directoryName("classes")
                .inFolder(false)
                .metaFile(true)
                .suffix("cls")
                .build();
                
        when(typeRepository.findByConnectionIdOrderByXmlNameAsc(connectionId))
                .thenReturn(List.of(mockType));

        List<MetadataTypeInfo> result = metadataService.describeMetadataTypes(connectionId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ApexClass", result.get(0).getXmlName());
        assertEquals("classes", result.get(0).getDirectoryName());
        
        verify(connectionRepository, never()).findById(anyString());
    }

    @Test
    void clearCache_deletesAllCacheForConnection() {
        String connectionId = "conn-123";

        metadataService.clearCache(connectionId);

        verify(typeRepository, times(1)).deleteByConnectionId(connectionId);
        verify(componentRepository, times(1)).deleteByConnectionId(connectionId);
    }
}
