package com.salesforce.workbench.repository;

import com.salesforce.workbench.entity.MetadataComponent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetadataComponentRepository extends JpaRepository<MetadataComponent, String> {
    List<MetadataComponent> findByConnectionIdAndTypeOrderByFullNameAsc(String connectionId, String type);
    boolean existsByConnectionIdAndType(String connectionId, String type);
    Page<MetadataComponent> findByConnectionIdAndTypeAndFullNameContainingIgnoreCaseOrderByFullNameAsc(String connectionId, String type, String search, Pageable pageable);
    void deleteByConnectionId(String connectionId);
}
