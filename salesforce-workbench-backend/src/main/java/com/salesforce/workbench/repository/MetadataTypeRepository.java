package com.salesforce.workbench.repository;

import com.salesforce.workbench.entity.MetadataType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetadataTypeRepository extends JpaRepository<MetadataType, String> {
    List<MetadataType> findByConnectionIdOrderByXmlNameAsc(String connectionId);
    void deleteByConnectionId(String connectionId);
}
