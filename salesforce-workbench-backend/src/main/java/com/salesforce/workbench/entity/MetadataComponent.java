package com.salesforce.workbench.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "metadata_components")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataComponent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "connection_id", nullable = false)
    private String connectionId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "full_name", nullable = false, length = 1000)
    private String fullName;

    @Column(name = "file_name", length = 1000)
    private String fileName;

    @Column(name = "namespace_prefix")
    private String namespacePrefix;

    @Column(name = "salesforce_id")
    private String salesforceId;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_modified_by_name")
    private String lastModifiedByName;

    @Column(name = "created_by_name")
    private String createdByName;
}
